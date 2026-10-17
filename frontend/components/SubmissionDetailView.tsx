"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import SubmissionFeedback from "@/components/SubmissionFeedback";
import SubmissionPlacement from "@/components/SubmissionPlacement";
import SubmissionInterviews from "@/components/SubmissionInterviews";
import { ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t, tx } from "@/lib/messages";
import type { SubmissionDetail } from "@/lib/types";
import { confirmDialog } from "@/lib/confirm";
import { Button } from "@/components/shadcn/button";
import { Badge, badgeVariants } from "@/components/shadcn/badge";
import { Card } from "@/components/shadcn/card";
import { useBreadcrumbLabel } from "@/lib/breadcrumb";
import StatusPill from "@/components/StatusPill";
import { usePolling } from "@/lib/usePolling";

export default function SubmissionDetailView({ id }: { id: string }) {
  const { can } = useAuth();
  const canManage = can("SUBMISSIONS_MANAGE");
  const [submission, setSubmission] = useState<SubmissionDetail | null>(null);
  useBreadcrumbLabel(submission?.candidateName);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setSubmission(await api<SubmissionDetail>(`/submissions/${id}`));
    } catch (failure) {
      setError(failure);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  async function action(path: string, confirm?: string) {
    if (confirm && !(await confirmDialog(confirm))) return;
    setBusy(true);
    setError(null);
    try {
      setSubmission(await api<SubmissionDetail>(`/submissions/${id}/${path}`, { method: "POST" }));
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  const sending =
    submission !== null &&
    (submission.emailStatus === "QUEUED" || submission.interviews.some((row) => row.inviteStatus === "QUEUED"));
  usePolling(sending, load);

  if (!submission) {
    return (
      <section>
        <ErrorBanner error={error} />
        {!error && <Loading />}
      </section>
    );
  }

  const emailProblem = submission.emailStatus === "FAILED" || submission.emailStatus === "NOT_CONFIGURED";

  return (
    <section>
      <div className="page-head">
        <h1>
          {submission.candidateName} <StatusPill kind="submissionStatus" value={submission.status} />
        </h1>
        {canManage && submission.status !== "WITHDRAWN" && (
          <div className="actions">
            {submission.emailStatus !== "SENT" && submission.emailStatus !== "QUEUED" && (
              <Button type="button" disabled={busy} onClick={() => void action("resend")}>
                {t("submissions.resend")}
              </Button>
            )}
            <Button
              variant="outline"
              type="button"
              disabled={busy}
              onClick={() => void action("withdraw", t("submissions.confirmWithdraw"))}
            >
              {t("submissions.withdraw")}
            </Button>
          </div>
        )}
      </div>
      <ErrorBanner error={error} />
      <div className="page-grid">
        <Card asChild>
          <div className="form-card">
            <dl className="dl-grid">
              <dt>{t("jobs.title")}</dt>
              <dd>
                <Link href={`/jobs/${submission.jobId}`}>
                  {submission.jobCode} · {submission.jobTitle}
                </Link>
              </dd>
              <dt>{t("jobs.client")}</dt>
              <dd>{submission.clientName}</dd>
              <dt>{t("candidates.fullName")}</dt>
              <dd>
                <Link href={`/candidates/${submission.candidateId}`}>{submission.candidateName}</Link> ·{" "}
                {submission.candidateEmail}
                {submission.candidatePhone ? ` · ${submission.candidatePhone}` : ""}
              </dd>
              <dt>{t("submissions.recipients")}</dt>
              <dd>{submission.recipients.join(", ")}</dd>
              <dt>{t("submissions.submittedAt")}</dt>
              <dd>
                {formatDate(submission.submittedAt)}
                {submission.submittedBy ? ` · ${submission.submittedBy}` : ""}
              </dd>
              <dt>{t("submissions.email")}</dt>
              <dd>
                <StatusPill kind="emailStatus" value={submission.emailStatus} />
                {submission.emailSentAt && <span className="muted"> {formatDate(submission.emailSentAt)}</span>}
                {submission.emailError && <div className="muted small">{submission.emailError}</div>}
              </dd>
              {submission.note && (
                <>
                  <dt>{t("submissions.note")}</dt>
                  <dd>{submission.note}</dd>
                </>
              )}
            </dl>
          </div>
        </Card>
        {submission.status === "SELECTED" && <SubmissionPlacement submissionId={submission.id} />}
      </div>
      <SubmissionInterviews submission={submission} canManage={canManage} onChanged={() => void load()} />
      <SubmissionFeedback submission={submission} canManage={canManage} onChanged={setSubmission} />
    </section>
  );
}
