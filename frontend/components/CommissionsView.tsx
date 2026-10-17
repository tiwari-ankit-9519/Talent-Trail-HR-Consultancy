"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Empty, ErrorBanner, Field, Loading, Pager } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDay, formatMoney, t, tx } from "@/lib/messages";
import type { CommissionStatus, CommissionView, Page } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Alert } from "@/components/shadcn/alert";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";
import { DateField } from "@/components/shadcn/date-picker";

const statuses: CommissionStatus[] = ["PENDING", "PAYABLE", "PAID", "CANCELLED"];

function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

type Editing = { id: string; kind: "amount" | "pay" } | null;

export default function CommissionsView() {
  const { can } = useAuth();
  const canManage = can("COMMISSIONS_MANAGE");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<Page<CommissionView> | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [localError, setLocalError] = useState("");
  const [editing, setEditing] = useState<Editing>(null);
  const [amount, setAmount] = useState("");
  const [paidOn, setPaidOn] = useState(today());
  const [reference, setReference] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setResult(
        await api<Page<CommissionView>>("/commissions", { query: { status: status || undefined, page, size: 20 } }),
      );
    } catch (failure) {
      setError(failure);
    }
  }, [status, page]);

  useEffect(() => {
    void load();
  }, [load]);

  function startAmount(row: CommissionView) {
    setError(null);
    setLocalError("");
    setAmount(row.amount === null ? "" : String(row.amount));
    setEditing({ id: row.id, kind: "amount" });
  }

  function startPay(row: CommissionView) {
    setError(null);
    setLocalError("");
    setPaidOn(today());
    setReference("");
    setEditing({ id: row.id, kind: "pay" });
  }

  async function submit(event: FormEvent, row: CommissionView) {
    event.preventDefault();
    if (!editing) return;
    setLocalError("");
    setBusy(true);
    setError(null);
    try {
      if (editing.kind === "amount") {
        const value = Number(amount);
        if (value < row.minAmount || value > row.maxAmount) {
          setLocalError(t("errors.COMMISSION_AMOUNT_RANGE"));
          return;
        }
        await api<CommissionView>(`/commissions/${row.id}/amount`, {
          method: "PUT",
          body: { amount: value, version: row.version },
        });
      } else {
        await api<CommissionView>(`/commissions/${row.id}/pay`, {
          method: "POST",
          body: { paidOn, reference, version: row.version },
        });
      }
      setEditing(null);
      await load();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <h1>{t("commissions.pageHeading")}</h1>
      <p className="muted">{t("commissions.help")}</p>
      <div className="toolbar">
        <FormSelect
          value={status}
          aria-label={t("common.status")}
          onChange={(e) => {
            setPage(0);
            setStatus(e.target.value);
          }}
        >
          <option value="">{t("common.allStatuses")}</option>
          {statuses.map((value) => (
            <option key={value} value={value}>
              {tx(`enums.commissionStatus.${value}`)}
            </option>
          ))}
        </FormSelect>
      </div>
      <ErrorBanner error={error} />
      {localError && (
        <Alert variant="destructive">
          <p>{localError}</p>
        </Alert>
      )}
      {!result && !error && <Loading />}
      {result && result.items.length === 0 && <Empty />}
      {result && result.items.length > 0 && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("commissions.recruiter")}</TableHead>
                <TableHead>{t("candidates.fullName")}</TableHead>
                <TableHead>{t("jobs.client")}</TableHead>
                <TableHead>{t("commissions.amount")}</TableHead>
                <TableHead>{t("commissions.payableAfter")}</TableHead>
                <TableHead>{t("commissions.invoice")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("commissions.waiting")}</TableHead>
                {canManage && <TableHead />}
              </TableRow>
            </TableHeader>
            <TableBody>
              {result.items.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{row.recruiterName}</TableCell>
                  <TableCell>
                    <Link href={`/placements/${row.placementId}`}>{row.candidateName}</Link> · {row.jobCode}
                  </TableCell>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>{row.amount === null ? t("commissions.amountNotSet") : formatMoney(row.amount)}</TableCell>
                  <TableCell>{formatDay(row.payableAfter)}</TableCell>
                  <TableCell>
                    {row.invoiceNumber
                      ? `${row.invoiceNumber} · ${tx(`enums.invoiceStatus.${row.invoiceStatus}`)}`
                      : t("commissions.noInvoice")}
                  </TableCell>
                  <TableCell>
                    <StatusPill kind="commissionStatus" value={row.status} />
                    <span className="muted small">
                      {row.status === "PAID" && row.paidOn ? ` ${formatDay(row.paidOn)}` : ""}
                      {row.status === "CANCELLED" && row.cancelReason ? ` ${row.cancelReason}` : ""}
                    </span>
                  </TableCell>
                  <TableCell>{row.blockers.map((b) => tx(`commissions.blocker.${b}`)).join(", ")}</TableCell>
                  {canManage && (
                    <TableCell>
                      {editing?.id === row.id ? (
                        <form onSubmit={(e) => void submit(e, row)}>
                          {editing.kind === "amount" ? (
                            <Field
                              label={t("commissions.amount")}
                              hint={`${formatMoney(row.minAmount)} – ${formatMoney(row.maxAmount)}`}
                            >
                              <Input
                                type="number"
                                required
                                min={row.minAmount}
                                max={row.maxAmount}
                                step="0.01"
                                value={amount}
                                onChange={(e) => setAmount(e.target.value)}
                              />
                            </Field>
                          ) : (
                            <>
                              <Field label={t("commissions.paidOn")}>
                                <DateField
                                  required
                                  max={today()}
                                  value={paidOn}
                                  onChange={(e) => setPaidOn(e.target.value)}
                                />
                              </Field>
                              <Field label={t("commissions.reference")}>
                                <Input
                                  maxLength={100}
                                  value={reference}
                                  onChange={(e) => setReference(e.target.value)}
                                />
                              </Field>
                            </>
                          )}
                          <div className="actions">
                            <Button type="submit" disabled={busy}>
                              {t("common.save")}
                            </Button>
                            <Button variant="outline" type="button" disabled={busy} onClick={() => setEditing(null)}>
                              {t("common.cancel")}
                            </Button>
                          </div>
                        </form>
                      ) : (
                        (row.status === "PENDING" || row.status === "PAYABLE") && (
                          <div className="actions">
                            <Button variant="outline" type="button" onClick={() => startAmount(row)}>
                              {t("commissions.setAmount")}
                            </Button>
                            {row.status === "PAYABLE" && (
                              <Button type="button" onClick={() => startPay(row)}>
                                {t("commissions.markPaid")}
                              </Button>
                            )}
                          </div>
                        )
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      <Pager page={result} onChange={setPage} />
    </section>
  );
}
