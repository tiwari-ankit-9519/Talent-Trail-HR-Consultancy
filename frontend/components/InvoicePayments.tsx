"use client";

import { useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDay, formatMoney, t, tx } from "@/lib/messages";
import type { InvoiceDetail, PaymentMode } from "@/lib/types";
import { confirmDialog } from "@/lib/confirm";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { DateField } from "@/components/shadcn/date-picker";

const modes: PaymentMode[] = ["BANK_TRANSFER", "UPI", "CHEQUE", "CASH", "OTHER"];

function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

type Props = {
  invoice: InvoiceDetail;
  canManage: boolean;
  onChanged: (invoice: InvoiceDetail) => void;
};

export default function InvoicePayments({ invoice, canManage, onChanged }: Props) {
  const [adding, setAdding] = useState(false);
  const [paidOn, setPaidOn] = useState(today());
  const [amount, setAmount] = useState("");
  const [tds, setTds] = useState("0");
  const [mode, setMode] = useState<PaymentMode>("BANK_TRANSFER");
  const [reference, setReference] = useState("");
  const [note, setNote] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [localError, setLocalError] = useState("");
  const [busy, setBusy] = useState(false);

  const cancelled = invoice.status === "CANCELLED";

  function paymentsTds(): number {
    return invoice.payments.reduce((sum, p) => sum + p.tdsDeducted, 0);
  }

  function start() {
    const expectedTds = Math.min(Math.max(invoice.tdsAmount - paymentsTds(), 0), invoice.balance);
    setPaidOn(today());
    setTds(expectedTds.toFixed(2));
    setAmount(Math.max(invoice.balance - expectedTds, 0).toFixed(2));
    setMode("BANK_TRANSFER");
    setReference("");
    setNote("");
    setError(null);
    setLocalError("");
    setAdding(true);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLocalError("");
    const received = Number(amount || 0);
    const deducted = Number(tds || 0);
    if (received + deducted <= 0) {
      setLocalError(t("errors.PAYMENT_AMOUNT_REQUIRED"));
      return;
    }
    if (received + deducted > invoice.balance + 0.001) {
      setLocalError(t("errors.PAYMENT_EXCEEDS_BALANCE"));
      return;
    }
    if (paidOn > today() || paidOn < invoice.invoiceDate) {
      setLocalError(t("errors.PAYMENT_DATE_INVALID"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const result = await api<InvoiceDetail>(`/invoices/${invoice.id}/payments`, {
        method: "POST",
        body: { paidOn, amountReceived: received, tdsDeducted: deducted, mode, reference, note },
      });
      onChanged(result);
      setAdding(false);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function remove(paymentId: string) {
    if (!(await confirmDialog(t("invoices.confirmRemovePayment")))) return;
    setBusy(true);
    setError(null);
    try {
      onChanged(await api<InvoiceDetail>(`/invoices/${invoice.id}/payments/${paymentId}`, { method: "DELETE" }));
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card asChild>
      <div className="form-card">
        <div className="page-head">
          <h2>{t("invoices.payments")}</h2>
          {canManage && !cancelled && invoice.balance > 0 && !adding && (
            <Button type="button" onClick={start}>
              {t("invoices.recordPayment")}
            </Button>
          )}
        </div>
        {!adding && <ErrorBanner error={error} />}
        {adding && (
          <form onSubmit={(e) => void submit(e)}>
            <ErrorBanner error={error} />
            {localError && (
              <Alert variant="destructive">
                <p>{localError}</p>
              </Alert>
            )}
            <div className="form-grid">
              <Field label={t("invoices.paidOn")}>
                <DateField
                  required
                  min={invoice.invoiceDate}
                  max={today()}
                  value={paidOn}
                  onChange={(e) => setPaidOn(e.target.value)}
                />
              </Field>
              <Field label={t("invoices.amountReceived")}>
                <Input type="number" min={0} step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />
              </Field>
              <Field label={t("invoices.tdsDeducted")} hint={t("invoices.tdsDeductedHint")}>
                <Input type="number" min={0} step="0.01" value={tds} onChange={(e) => setTds(e.target.value)} />
              </Field>
              <Field label={t("invoices.mode")}>
                <FormSelect value={mode} onChange={(e) => setMode(e.target.value as PaymentMode)}>
                  {modes.map((value) => (
                    <option key={value} value={value}>
                      {tx(`enums.paymentMode.${value}`)}
                    </option>
                  ))}
                </FormSelect>
              </Field>
              <Field label={t("invoices.reference")}>
                <Input maxLength={100} value={reference} onChange={(e) => setReference(e.target.value)} />
              </Field>
            </div>
            <Field label={t("invoices.note")}>
              <Input maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
            </Field>
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("common.save")}
              </Button>
              <Button variant="outline" type="button" disabled={busy} onClick={() => setAdding(false)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        )}
        {invoice.payments.length === 0 ? (
          <p className="muted">{t("invoices.noPayments")}</p>
        ) : (
          <div className="table-wrap">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("invoices.paidOn")}</TableHead>
                  <TableHead>{t("invoices.amountReceived")}</TableHead>
                  <TableHead>{t("invoices.tdsDeducted")}</TableHead>
                  <TableHead>{t("invoices.mode")}</TableHead>
                  <TableHead>{t("invoices.reference")}</TableHead>
                  <TableHead>{t("invoices.note")}</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {invoice.payments.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell>{formatDay(p.paidOn)}</TableCell>
                    <TableCell>{formatMoney(p.amountReceived)}</TableCell>
                    <TableCell>{formatMoney(p.tdsDeducted)}</TableCell>
                    <TableCell>{tx(`enums.paymentMode.${p.mode}`)}</TableCell>
                    <TableCell>{p.reference}</TableCell>
                    <TableCell>{p.note}</TableCell>
                    <TableCell>
                      {canManage && !cancelled && (
                        <Button variant="outline" type="button" disabled={busy} onClick={() => void remove(p.id)}>
                          {t("common.delete")}
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
    </Card>
  );
}
