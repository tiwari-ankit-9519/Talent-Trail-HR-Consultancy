"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { ErrorBanner } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDay, formatMoney, t, tx } from "@/lib/messages";
import type { InvoiceDetail } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";

type Props = {
  placementId: string;
  proofAttached: boolean;
};

export default function PlacementInvoice({ placementId, proofAttached }: Props) {
  const { can } = useAuth();
  const canView = can("INVOICES_VIEW") || can("INVOICES_MANAGE");
  const canManage = can("INVOICES_MANAGE");
  const [invoice, setInvoice] = useState<InvoiceDetail | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!canView) return;
    try {
      setError(null);
      setInvoice((await api<InvoiceDetail | undefined>(`/placements/${placementId}/invoice`)) ?? null);
      setLoaded(true);
    } catch (failure) {
      setError(failure);
    }
  }, [placementId, canView]);

  useEffect(() => {
    void load();
  }, [load, proofAttached]);

  async function raise() {
    setBusy(true);
    setError(null);
    try {
      setInvoice(await api<InvoiceDetail>("/invoices", { method: "POST", body: { placementId } }));
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  if (!canView) return null;

  return (
    <Card asChild>
      <div className="form-card">
        <h2>{t("invoices.heading")}</h2>
        <ErrorBanner error={error} />
        {invoice ? (
          <p>
            <Link href={`/invoices/${invoice.id}`}>{invoice.number}</Link> · {formatMoney(invoice.totalAmount)} ·{" "}
            {tx(`enums.invoiceStatus.${invoice.status}`)} · {t("invoices.dueDate")} {formatDay(invoice.dueDate)}
          </p>
        ) : (
          loaded && (
            <>
              <p className="muted small">{proofAttached ? t("invoices.readyToRaise") : t("invoices.blockedNoProof")}</p>
              {canManage && (
                <div className="actions">
                  <Button type="button" disabled={busy || !proofAttached} onClick={() => void raise()}>
                    {t("invoices.raise")}
                  </Button>
                </div>
              )}
            </>
          )
        )}
      </div>
    </Card>
  );
}
