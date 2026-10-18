"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import CandidateApplications from "@/components/CandidateApplications";
import CandidateConsent from "@/components/CandidateConsent";
import CandidateForm from "@/components/CandidateForm";
import CandidateResume from "@/components/CandidateResume";
import { ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { t, tx } from "@/lib/messages";
import type { CandidateDetail, CandidateStatus } from "@/lib/types";
import { FormSelect } from "@/components/shadcn/select";
import { Badge } from "@/components/shadcn/badge";
import { Alert } from "@/components/shadcn/alert";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { useBreadcrumbLabel } from "@/lib/breadcrumb";
import StatusPill from "@/components/StatusPill";
import Timeline from "@/components/Timeline";

type Tab = "profile" | "consent" | "jobs" | "history";

const statuses: CandidateStatus[] = ["NEW", "SCREENING", "AVAILABLE", "NOT_INTERESTED", "PLACED", "BLOCKED"];

export default function CandidateDetailView({ id }: { id: string }) {
  const { can } = useAuth();
  const router = useRouter();
  const isNew = id === "new";
  const canManage = can("CANDIDATES_MANAGE");
  const [candidate, setCandidate] = useState<CandidateDetail | null>(null);
  useBreadcrumbLabel(candidate?.fullName);
  const [tab, setTab] = useState<Tab>("profile");
  const [error, setError] = useState<unknown>(null);
  const [formKey, setFormKey] = useState(0);

  const load = useCallback(async () => {
    if (isNew) return;
    try {
      setError(null);
      setCandidate(await api<CandidateDetail>(`/candidates/${id}`));
      setFormKey((k) => k + 1);
    } catch (failure) {
      setError(failure);
    }
  }, [id, isNew]);

  useEffect(() => {
    void load();
  }, [load]);

  function replace(next: CandidateDetail) {
    setCandidate(next);
    setFormKey((k) => k + 1);
  }

  async function changeStatus(status: string) {
    if (!candidate) return;
    setError(null);
    try {
      replace(
        await api<CandidateDetail>(`/candidates/${candidate.id}/status`, {
          method: "PATCH",
          body: { status },
        }),
      );
    } catch (failure) {
      setError(failure);
    }
  }

  if (isNew) {
    return (
      <section>
        <h1>{t("candidates.add")}</h1>
        {canManage ? (
          <CandidateForm
            candidate={null}
            canManage
            onSaved={(created) => router.replace(`/candidates/${created.id}`)}
          />
        ) : (
          <Alert variant="destructive">{t("errors.FORBIDDEN")}</Alert>
        )}
      </section>
    );
  }

  if (!candidate) {
    return (
      <section>
        <ErrorBanner error={error} />
        {!error && <Loading />}
      </section>
    );
  }

  return (
    <section>
      <div className="page-head">
        <h1>{candidate.fullName}</h1>
        {canManage ? (
          <FormSelect value={candidate.status} onChange={(e) => void changeStatus(e.target.value)}>
            {statuses.map((value) => (
              <option key={value} value={value}>
                {tx(`enums.candidateStatus.${value}`)}
              </option>
            ))}
          </FormSelect>
        ) : (
          <StatusPill kind="candidateStatus" value={candidate.status} />
        )}
      </div>
      <ErrorBanner error={error} />
      <Tabs value={tab} onValueChange={(value) => setTab(value as typeof tab)}>
        <TabsList>
          <TabsTrigger value="profile">{t("candidates.tabProfile")}</TabsTrigger>
          <TabsTrigger value="consent">{t("candidates.tabConsent")}</TabsTrigger>
          <TabsTrigger value="jobs">{t("candidates.tabJobs")}</TabsTrigger>
          <TabsTrigger value="history">{t("candidates.tabHistory")}</TabsTrigger>
        </TabsList>
      </Tabs>
      {tab === "profile" && (
        <div className="page-grid">
          <CandidateForm key={formKey} candidate={candidate} canManage={canManage} onSaved={setCandidate} />
          <CandidateResume candidate={candidate} canManage={canManage} onChanged={replace} />
        </div>
      )}
      {tab === "consent" && <CandidateConsent candidate={candidate} canManage={canManage} onChanged={replace} />}
      {tab === "jobs" && <CandidateApplications candidate={candidate} canManage={canManage} onChanged={replace} />}
      {tab === "history" && <Timeline subject="CANDIDATE" id={candidate.id} />}
    </section>
  );
}
