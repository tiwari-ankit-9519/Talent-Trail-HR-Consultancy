"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { api } from "@/lib/api";
import { formatDay, formatMoney, t, tx } from "@/lib/messages";
import type { CommissionView } from "@/lib/types";
import Link from "next/link";
import { Badge } from "@/components/shadcn/badge";
import { Card } from "@/components/shadcn/card";
import StatusPill from "@/components/StatusPill";

export default function PlacementCommission({ placementId }: { placementId: string }) {
  const { can } = useAuth();
  const allowed = can("COMMISSIONS_VIEW") || can("COMMISSIONS_VIEW_OWN") || can("COMMISSIONS_MANAGE");
  const [commission, setCommission] = useState<CommissionView | null>(null);

  useEffect(() => {
    if (!allowed) return;
    void (async () => {
      try {
        setCommission((await api<CommissionView | undefined>(`/placements/${placementId}/commission`)) ?? null);
      } catch {
        setCommission(null);
      }
    })();
  }, [placementId, allowed]);

  if (!allowed || !commission) return null;

  return (
    <Card asChild>
      <div className="form-card">
        <h2>{t("commissions.heading")}</h2>
        <p>
          {commission.recruiterName} ·{" "}
          {commission.amount === null ? t("commissions.amountNotSet") : formatMoney(commission.amount)} ·{" "}
          <StatusPill kind="commissionStatus" value={commission.status} /> · {t("commissions.payableAfter")}{" "}
          {formatDay(commission.payableAfter)} · <Link href="/commissions">{t("commissions.open")}</Link>
        </p>
      </div>
    </Card>
  );
}
