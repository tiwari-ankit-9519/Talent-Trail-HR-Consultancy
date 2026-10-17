"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import JobForm from "@/components/JobForm";
import SubmissionsView from "@/components/SubmissionsView";
import SubmitCandidateForm from "@/components/SubmitCandidateForm";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { t, tx } from "@/lib/messages";
import type { CandidateSummary, JobDetail, JobStatus } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { Badge } from "@/components/shadcn/badge";
import { Alert } from "@/components/shadcn/alert";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { useBreadcrumbLabel } from "@/lib/breadcrumb";
import StatusPill from "@/components/StatusPill";

type Tab = "details" | "candidates" | "submissions";

const transitions: Record<JobStatus, JobStatus[]> = {
  DRAFT: ["OPEN", "CLOSED"],
  OPEN: ["ON_HOLD", "CLOSED"],
  ON_HOLD: ["OPEN", "CLOSED"],
  CLOSED: ["OPEN"],
};

export default function JobDetailView({ id }: { id: string }) {
  const { can } = useAuth();
  const router = useRouter();
  const isNew = id === "new";
  const canManage = can("JOBS_MANAGE");
  const canSeeCandidates = can("CANDIDATES_VIEW", "CANDIDATES_MANAGE");
  const canSeeSubmissions = can("SUBMISSIONS_VIEW", "SUBMISSIONS_MANAGE");
  const canSubmit = can("SUBMISSIONS_MANAGE");
  const [job, setJob] = useState<JobDetail | null>(null);
  useBreadcrumbLabel(job ? job.code : null);
  const [candidates, setCandidates] = useState<CandidateSummary[] | null>(null);
  const [tab, setTab] = useState<Tab>("details");
  const [error, setError] = useState<unknown>(null);
  const [formKey, setFormKey] = useState(0);
  const [submitting, setSubmitting] = useState<CandidateSummary | null>(null);
  const [submissionsKey, setSubmissionsKey] = useState(0);

  const load = useCallback(async () => {
    if (isNew) return;
    try {
      setError(null);
      setJob(await api<JobDetail>(`/jobs/${id}`));
      setFormKey((k) => k + 1);
    } catch (failure) {
      setError(failure);
    }
  }, [id, isNew]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (tab !== "candidates" || isNew || !canSeeCandidates) return;
    api<CandidateSummary[]>(`/jobs/${id}/candidates`).then(setCandidates).catch(setError);
  }, [tab, id, isNew, canSeeCandidates]);

  async function move(status: JobStatus) {
    if (!job) return;
    setError(null);
    try {
      setJob(await api<JobDetail>(`/jobs/${job.id}/status`, { method: "PATCH", body: { status } }));
      setFormKey((k) => k + 1);
    } catch (failure) {
      setError(failure);
    }
  }

  if (isNew) {
    return (
      <section>
        <h1>{t("jobs.add")}</h1>
        {canManage ? (
          <JobForm job={null} canManage onSaved={(created) => router.replace(`/jobs/${created.id}`)} />
        ) : (
          <Alert variant="destructive">{t("errors.FORBIDDEN")}</Alert>
        )}
      </section>
    );
  }

  if (!job) {
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
        <h1>
          {job.code} · {job.title} <StatusPill kind="jobStatus" value={job.status} />
        </h1>
        {canManage && (
          <div className="actions">
            {transitions[job.status].map((next) => (
              <Button variant="outline" key={next} type="button" onClick={() => void move(next)}>
                {t("jobs.moveTo")} {tx(`enums.jobStatus.${next}`)}
              </Button>
            ))}
          </div>
        )}
      </div>
      <p className="muted">
        {job.clientName} · {t("jobs.applicants")}: {job.applicantCount}
        {job.published && job.status === "OPEN" && (
          <>
            {" · "}
            <Link href={`/apply/${job.code}`} target="_blank">
              {t("jobs.viewApplyPage")}
            </Link>
          </>
        )}
      </p>
      <ErrorBanner error={error} />
      <Tabs value={tab} onValueChange={(value) => setTab(value as typeof tab)}>
        <TabsList>
          <TabsTrigger value="details">{t("jobs.tabDetails")}</TabsTrigger>
          {canSeeCandidates && <TabsTrigger value="candidates">{t("jobs.tabCandidates")}</TabsTrigger>}
          {canSeeSubmissions && <TabsTrigger value="submissions">{t("jobs.tabSubmissions")}</TabsTrigger>}
        </TabsList>
      </Tabs>
      {tab === "submissions" && canSeeSubmissions && <SubmissionsView jobId={job.id} refreshKey={submissionsKey} />}
      {tab === "details" && <JobForm key={formKey} job={job} canManage={canManage} onSaved={setJob} />}
      {tab === "candidates" && submitting && (
        <SubmitCandidateForm
          jobId={job.id}
          candidate={submitting}
          onCancel={() => setSubmitting(null)}
          onDone={(created) => {
            setSubmitting(null);
            setSubmissionsKey((k) => k + 1);
            router.push(`/submissions/${created.id}`);
          }}
        />
      )}
      {tab === "candidates" &&
        (!candidates ? (
          <Loading />
        ) : candidates.length === 0 ? (
          <Empty />
        ) : (
          <div className="table-wrap">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("candidates.fullName")}</TableHead>
                  <TableHead>{t("candidates.currentTitle")}</TableHead>
                  <TableHead>{t("candidates.experience")}</TableHead>
                  <TableHead>{t("common.status")}</TableHead>
                  <TableHead>{t("candidates.consent")}</TableHead>
                  {canSubmit && job.status === "OPEN" && <TableHead>{t("common.actions")}</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {candidates.map((candidate) => (
                  <TableRow key={candidate.id}>
                    <TableCell>
                      <Link href={`/candidates/${candidate.id}`}>{candidate.fullName}</Link>
                    </TableCell>
                    <TableCell>{candidate.currentTitle}</TableCell>
                    <TableCell>{candidate.totalExperienceYears}</TableCell>
                    <TableCell>
                      <StatusPill kind="candidateStatus" value={candidate.status} />
                    </TableCell>
                    <TableCell>
                      <StatusPill kind="consentStatus" value={candidate.consentStatus} />
                    </TableCell>
                    {canSubmit && job.status === "OPEN" && (
                      <TableCell>
                        {candidate.consentStatus === "GRANTED" && candidate.status !== "BLOCKED" ? (
                          <Button variant="outline" type="button" onClick={() => setSubmitting(candidate)}>
                            {t("submissions.submitToClient")}
                          </Button>
                        ) : (
                          <span className="muted small">{t("submissions.needsConsent")}</span>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        ))}
    </section>
  );
}
