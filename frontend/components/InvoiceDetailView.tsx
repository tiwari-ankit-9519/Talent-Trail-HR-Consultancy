"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import InvoicePayments from "@/components/InvoicePayments";
import Timeline from "@/components/Timeline";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api, apiBlob } from "@/lib/api";
import { formatDate, formatDay, formatMoney, t, tx } from "@/lib/messages";
import type { InvoiceDetail } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Badge, badgeVariants } from "@/components/shadcn/badge";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";
import { useBreadcrumbLabel } from "@/lib/breadcrumb";
import StatusPill from "@/components/StatusPill";
import { usePolling } from "@/lib/usePolling";

export default function InvoiceDetailView({ id }: { id: string }) {
  const { can } = useAuth();
  const canManage = can("INVOICES_MANAGE");
  const [invoice, setInvoice] = useState<InvoiceDetail | null>(null);
  useBreadcrumbLabel(invoice?.number);
  const [error, setError] = useState<unknown>(null);
  const [sending, setSending] = useState(false);
  const [selected, setSelected] = useState<string[]>([]);
  const [extra, setExtra] = useState("");
  const [cancelling, setCancelling] = useState(false);
  const [reason, setReason] = useState("");
  const [localError, setLocalError] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setInvoice(await api<InvoiceDetail>(`/invoices/${id}`));
    } catch (failure) {
      setError(failure);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  function startSend() {
    if (!invoice) return;
    setError(null);
    setLocalError("");
    setExtra("");
    const primary = invoice.contacts.filter((c) => c.primary).map((c) => c.email);
    setSelected(primary.length > 0 ? primary : invoice.contacts.slice(0, 1).map((c) => c.email));
    setSending(true);
  }

  function toggle(email: string) {
    setSelected((current) => (current.includes(email) ? current.filter((e) => e !== email) : [...current, email]));
  }

  async function send(event: FormEvent) {
    event.preventDefault();
    if (!invoice) return;
    setLocalError("");
    const typed = extra
      .split(/[,;\s]+/)
      .map((e) => e.trim())
      .filter(Boolean);
    const to = Array.from(new Set([...selected, ...typed]));
    if (to.length === 0) {
      setLocalError(t("errors.INVOICE_RECIPIENT_REQUIRED"));
      return;
    }
    if (to.some((e) => !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e))) {
      setLocalError(t("invoices.invalidRecipient"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      setInvoice(await api<InvoiceDetail>(`/invoices/${id}/send`, { method: "POST", body: { to } }));
      setSending(false);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function cancel(event: FormEvent) {
    event.preventDefault();
    if (!invoice) return;
    setBusy(true);
    setError(null);
    try {
      setInvoice(
        await api<InvoiceDetail>(`/invoices/${id}/cancel`, {
          method: "POST",
          body: { reason, version: invoice.version },
        }),
      );
      setCancelling(false);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function download() {
    if (!invoice) return;
    setError(null);
    try {
      const { blob, fileName } = await apiBlob(`/invoices/${id}/pdf`);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (failure) {
      setError(failure);
    }
  }

  usePolling(invoice?.emailStatus === "QUEUED", load);

  if (!invoice) {
    return (
      <section>
        <ErrorBanner error={error} />
        {!error && <Loading />}
      </section>
    );
  }

  const cancelled = invoice.status === "CANCELLED";
  const half = invoice.gstRate / 2;

  return (
    <section>
      <div className="page-head">
        <h1>
          {invoice.number} <StatusPill kind="invoiceStatus" value={invoice.status} />
          {invoice.overdue && <Badge variant="warn">{t("invoices.overdue")}</Badge>}
        </h1>
        <div className="actions">
          <Button variant="outline" type="button" onClick={() => void download()} disabled={cancelled}>
            {t("invoices.downloadPdf")}
          </Button>
          {canManage && !cancelled && !sending && !cancelling && (
            <>
              <Button type="button" disabled={invoice.emailStatus === "QUEUED"} onClick={startSend}>
                {invoice.emailStatus === "SENT" ? t("invoices.sendAgain") : t("invoices.send")}
              </Button>
              {invoice.settledAmount === 0 && (
                <Button
                  variant="outline"
                  type="button"
                  onClick={() => {
                    setError(null);
                    setReason("");
                    setCancelling(true);
                  }}
                >
                  {t("invoices.cancelInvoice")}
                </Button>
              )}
            </>
          )}
        </div>
      </div>
      <ErrorBanner error={error} />

      {sending && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void send(e)}>
            <h2>{t("invoices.sendHeading")}</h2>
            {localError && (
              <Alert variant="destructive">
                <p>{localError}</p>
              </Alert>
            )}
            {invoice.contacts.length === 0 && <p className="muted">{t("invoices.noContacts")}</p>}
            <div className="checks">
              {invoice.contacts.map((c) => (
                <label key={c.email} className="check">
                  <CheckboxField checked={selected.includes(c.email)} onChange={() => toggle(c.email)} />
                  {c.name} · {c.email}
                </label>
              ))}
            </div>
            <Field label={t("invoices.otherRecipients")} hint={t("invoices.otherRecipientsHint")}>
              <Input value={extra} onChange={(e) => setExtra(e.target.value)} />
            </Field>
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("invoices.sendNow")}
              </Button>
              <Button variant="outline" type="button" disabled={busy} onClick={() => setSending(false)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {cancelling && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void cancel(e)}>
            <h2>{t("invoices.cancelInvoice")}</h2>
            <Field label={t("invoices.cancelReason")}>
              <Input required maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
            </Field>
            <p className="muted small">{t("invoices.cancelHelp")}</p>
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("invoices.confirmCancel")}
              </Button>
              <Button variant="outline" type="button" disabled={busy} onClick={() => setCancelling(false)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        </Card>
      )}

      <div className="page-grid">
        <Card asChild>
          <div className="form-card">
            <dl className="dl-grid">
              <dt>{t("jobs.client")}</dt>
              <dd>
                <Link href={`/clients/${invoice.clientId}`}>{invoice.clientName}</Link>
              </dd>
              <dt>{t("candidates.fullName")}</dt>
              <dd>
                <Link href={`/candidates/${invoice.candidateId}`}>{invoice.candidateName}</Link> ·{" "}
                <Link href={`/placements/${invoice.placementId}`}>{t("invoices.viewJoining")}</Link>
              </dd>
              <dt>{t("jobs.title")}</dt>
              <dd>
                <Link href={`/jobs/${invoice.jobId}`}>
                  {invoice.jobCode} · {invoice.jobTitle}
                </Link>
              </dd>
              <dt>{t("placements.joiningDate")}</dt>
              <dd>{formatDay(invoice.joiningDate)}</dd>
              <dt>{t("invoices.invoiceDate")}</dt>
              <dd>{formatDay(invoice.invoiceDate)}</dd>
              <dt>{t("invoices.dueDate")}</dt>
              <dd>{formatDay(invoice.dueDate)}</dd>
              <dt>{t("invoices.sac")}</dt>
              <dd>{invoice.sacCode}</dd>
              <dt>{t("placements.offeredCtc")}</dt>
              <dd>{formatMoney(invoice.offeredCtc)}</dd>
              <dt>{t("invoices.feeBasis")}</dt>
              <dd>
                {tx(`enums.feeType.${invoice.feeType}`)} · {invoice.feeValue}
              </dd>
              {cancelled && (
                <>
                  <dt>{t("invoices.cancelReason")}</dt>
                  <dd>
                    {invoice.cancelReason} ({formatDate(invoice.cancelledAt)})
                  </dd>
                </>
              )}
            </dl>
          </div>
        </Card>

        <Card asChild>
          <div className="form-card">
            <h2>{t("invoices.amounts")}</h2>
            <dl className="dl-grid">
              <dt>{t("invoices.baseAmount")}</dt>
              <dd>{formatMoney(invoice.baseAmount)}</dd>
              {invoice.cgstAmount > 0 && (
                <>
                  <dt>CGST {half}%</dt>
                  <dd>{formatMoney(invoice.cgstAmount)}</dd>
                  <dt>SGST {half}%</dt>
                  <dd>{formatMoney(invoice.sgstAmount)}</dd>
                </>
              )}
              {invoice.igstAmount > 0 && (
                <>
                  <dt>IGST {invoice.gstRate}%</dt>
                  <dd>{formatMoney(invoice.igstAmount)}</dd>
                </>
              )}
              <dt>{t("invoices.total")}</dt>
              <dd>
                <strong>{formatMoney(invoice.totalAmount)}</strong>
              </dd>
              {invoice.tdsAmount > 0 && (
                <>
                  <dt>TDS {invoice.tdsRate}%</dt>
                  <dd>{formatMoney(invoice.tdsAmount)}</dd>
                  <dt>{t("invoices.netReceivable")}</dt>
                  <dd>{formatMoney(invoice.netReceivable)}</dd>
                </>
              )}
              <dt>{t("invoices.settled")}</dt>
              <dd>{formatMoney(invoice.settledAmount)}</dd>
              <dt>{t("invoices.balance")}</dt>
              <dd>
                <strong>{formatMoney(invoice.balance)}</strong>
              </dd>
            </dl>
          </div>
        </Card>

        <Card asChild>
          <div className="form-card">
            <h2>{t("invoices.emailHeading")}</h2>
            <p>
              <StatusPill kind="emailStatus" value={invoice.emailStatus} />
              {invoice.emailSentAt && <span className="muted"> {formatDate(invoice.emailSentAt)}</span>}
            </p>
            {invoice.sentTo && <p className="muted small">{invoice.sentTo}</p>}
            {invoice.emailError && <p className="muted small">{invoice.emailError}</p>}
          </div>
        </Card>
      </div>

      <InvoicePayments invoice={invoice} canManage={canManage} onChanged={setInvoice} />
      <div className="mt-4">
        <Timeline subject="INVOICE" id={invoice.id} refreshKey={`${invoice.status}-${invoice.balance}`} />
      </div>
    </section>
  );
}
