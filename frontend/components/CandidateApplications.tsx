"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { ErrorBanner } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t, tx } from "@/lib/messages";
import type { CandidateDetail, JobSummary, Page } from "@/lib/types";
import { confirmDialog } from "@/lib/confirm";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";

type Props = {
  candidate: CandidateDetail;
  canManage: boolean;
  onChanged: (candidate: CandidateDetail) => void;
};

export default function CandidateApplications({ candidate, canManage, onChanged }: Props) {
  const { can } = useAuth();
  const canSeeJobs = can("JOBS_VIEW", "JOBS_MANAGE");
  const [jobs, setJobs] = useState<JobSummary[]>([]);
  const [jobId, setJobId] = useState("");
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    if (!canManage || !canSeeJobs) return;
    api<Page<JobSummary>>("/jobs", { query: { size: 100 } })
      .then((result) => setJobs(result.items.filter((job) => job.status !== "CLOSED")))
      .catch(setError);
  }, [canManage, canSeeJobs]);

  const linked = new Set(candidate.applications.map((a) => a.jobId));
  const options = jobs.filter((job) => !linked.has(job.id));

  async function link() {
    if (!jobId) return;
    setError(null);
    try {
      onChanged(
        await api<CandidateDetail>(`/candidates/${candidate.id}/applications`, {
          method: "POST",
          body: { jobId },
        }),
      );
      setJobId("");
    } catch (failure) {
      setError(failure);
    }
  }

  async function unlink(applicationId: string) {
    if (!(await confirmDialog(t("applications.confirmUnlink")))) return;
    setError(null);
    try {
      onChanged(
        await api<CandidateDetail>(`/candidates/${candidate.id}/applications/${applicationId}`, {
          method: "DELETE",
        }),
      );
    } catch (failure) {
      setError(failure);
    }
  }

  return (
    <div className="section-block">
      <ErrorBanner error={error} />
      {canManage && canSeeJobs && (
        <div className="toolbar">
          <FormSelect value={jobId} onChange={(e) => setJobId(e.target.value)}>
            <option value="">{t("applications.selectJob")}</option>
            {options.map((job) => (
              <option key={job.id} value={job.id}>
                {job.code} · {job.title} · {job.clientName}
              </option>
            ))}
          </FormSelect>
          <Button type="button" disabled={!jobId} onClick={() => void link()}>
            {t("applications.link")}
          </Button>
        </div>
      )}
      {candidate.applications.length === 0 ? (
        <p className="muted">{t("applications.none")}</p>
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("jobs.code")}</TableHead>
                <TableHead>{t("jobs.title")}</TableHead>
                <TableHead>{t("jobs.client")}</TableHead>
                <TableHead>{t("candidates.source")}</TableHead>
                <TableHead>{t("applications.appliedAt")}</TableHead>
                {canManage && <TableHead>{t("common.actions")}</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {candidate.applications.map((application) => (
                <TableRow key={application.id}>
                  <TableCell>
                    {canSeeJobs ? (
                      <Link href={`/jobs/${application.jobId}`}>{application.jobCode}</Link>
                    ) : (
                      application.jobCode
                    )}
                  </TableCell>
                  <TableCell>{application.jobTitle}</TableCell>
                  <TableCell>{application.clientName}</TableCell>
                  <TableCell>{tx(`enums.candidateSource.${application.source}`)}</TableCell>
                  <TableCell>{formatDate(application.appliedAt)}</TableCell>
                  {canManage && (
                    <TableCell>
                      <Button variant="outline" type="button" onClick={() => void unlink(application.id)}>
                        {t("applications.unlink")}
                      </Button>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
