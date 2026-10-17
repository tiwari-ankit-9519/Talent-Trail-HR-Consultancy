"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import OfferForm from "@/components/OfferForm";
import { ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDay, formatMoney, t, tx } from "@/lib/messages";
import type { Page, PlacementDetail, PlacementSummary } from "@/lib/types";
import { Badge } from "@/components/shadcn/badge";
import { Card } from "@/components/shadcn/card";
import StatusPill from "@/components/StatusPill";

export default function SubmissionPlacement({ submissionId }: { submissionId: string }) {
  const { can } = useAuth();
  const canView = can("PLACEMENTS_VIEW", "PLACEMENTS_MANAGE");
  const canManage = can("PLACEMENTS_MANAGE");
  const [existing, setExisting] = useState<PlacementSummary | null | undefined>(undefined);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      const result = await api<Page<PlacementSummary>>("/placements", { query: { submissionId, size: 1 } });
      setExisting(result.items[0] ?? null);
    } catch (failure) {
      setError(failure);
    }
  }, [submissionId]);

  useEffect(() => {
    if (canView) void load();
  }, [canView, load]);

  if (!canView) return null;

  function created(placement: PlacementDetail) {
    setExisting({
      id: placement.id,
      submissionId: placement.submissionId,
      jobId: placement.jobId,
      jobCode: placement.jobCode,
      jobTitle: placement.jobTitle,
      clientId: placement.clientId,
      clientName: placement.clientName,
      candidateId: placement.candidateId,
      candidateName: placement.candidateName,
      status: placement.status,
      offeredCtc: placement.offeredCtc,
      expectedJoiningDate: placement.expectedJoiningDate,
      joiningDate: placement.joiningDate,
      joiningEmailAttached: placement.joiningEmailAttached,
    });
  }

  return (
    <div className="section-block">
      <ErrorBanner error={error} />
      {existing === undefined && !error && <Loading />}
      {existing && (
        <Card asChild>
          <div className="form-card">
            <h2>{t("placements.heading")}</h2>
            <p>
              <StatusPill kind="placementStatus" value={existing.status} /> {formatMoney(existing.offeredCtc)} ·{" "}
              {t("placements.expectedJoiningDate")}: {formatDay(existing.expectedJoiningDate)}
            </p>
            <p>
              <Link href={`/placements/${existing.id}`}>{t("placements.open")}</Link>
            </p>
          </div>
        </Card>
      )}
      {existing === null && canManage && <OfferForm submissionId={submissionId} placement={null} onSaved={created} />}
    </div>
  );
}
