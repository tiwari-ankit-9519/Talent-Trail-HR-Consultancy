"use client";

import { useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t, tx } from "@/lib/messages";
import type { FeedbackDecision, SubmissionDetail } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Textarea } from "@/components/shadcn/textarea";
import { Card } from "@/components/shadcn/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { DateField } from "@/components/shadcn/date-picker";

type Props = {
  submission: SubmissionDetail;
  canManage: boolean;
  onChanged: (submission: SubmissionDetail) => void;
};

const decisions: FeedbackDecision[] = ["SHORTLISTED", "SELECTED", "ON_HOLD", "REJECTED"];

function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export default function SubmissionFeedback({ submission, canManage, onChanged }: Props) {
  const [decision, setDecision] = useState<FeedbackDecision>("SHORTLISTED");
  const [receivedOn, setReceivedOn] = useState(today());
  const [interviewId, setInterviewId] = useState("");
  const [comments, setComments] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const withdrawn = submission.status === "WITHDRAWN";

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await api<SubmissionDetail>(`/submissions/${submission.id}/feedback`, {
        method: "POST",
        body: { decision, comments, receivedOn, interviewId: interviewId || null },
      });
      setComments("");
      setInterviewId("");
      onChanged(result);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  const rounds = new Map(submission.interviews.map((i) => [i.id, i.roundNumber]));

  return (
    <div className="section-block">
      <h2>{t("feedback.heading")}</h2>
      {canManage && !withdrawn && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void submit(e)}>
            <p className="muted small">{t("feedback.help")}</p>
            <ErrorBanner error={error} />
            <div className="form-grid">
              <Field label={t("feedback.decision")}>
                <FormSelect value={decision} onChange={(e) => setDecision(e.target.value as FeedbackDecision)}>
                  {decisions.map((value) => (
                    <option key={value} value={value}>
                      {tx(`enums.feedbackDecision.${value}`)}
                    </option>
                  ))}
                </FormSelect>
              </Field>
              <Field label={t("feedback.receivedOn")}>
                <DateField required max={today()} value={receivedOn} onChange={(e) => setReceivedOn(e.target.value)} />
              </Field>
              <Field label={t("feedback.interview")}>
                <FormSelect value={interviewId} onChange={(e) => setInterviewId(e.target.value)}>
                  <option value="">{t("feedback.general")}</option>
                  {submission.interviews.map((interview) => (
                    <option key={interview.id} value={interview.id}>
                      {t("interviews.round")} {interview.roundNumber}
                    </option>
                  ))}
                </FormSelect>
              </Field>
            </div>
            <Field label={t("feedback.comments")} hint={t("feedback.commentsHint")}>
              <Textarea rows={3} maxLength={4000} value={comments} onChange={(e) => setComments(e.target.value)} />
            </Field>
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("feedback.save")}
              </Button>
            </div>
          </form>
        </Card>
      )}
      {submission.feedback.length === 0 ? (
        <p className="muted">{t("feedback.none")}</p>
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("feedback.receivedOn")}</TableHead>
                <TableHead>{t("feedback.decision")}</TableHead>
                <TableHead>{t("feedback.interview")}</TableHead>
                <TableHead>{t("feedback.comments")}</TableHead>
                <TableHead>{t("feedback.recordedBy")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {submission.feedback.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{row.receivedOn}</TableCell>
                  <TableCell>{tx(`enums.feedbackDecision.${row.decision}`)}</TableCell>
                  <TableCell>
                    {row.interviewId && rounds.has(row.interviewId)
                      ? `${t("interviews.round")} ${rounds.get(row.interviewId)}`
                      : t("feedback.general")}
                  </TableCell>
                  <TableCell>{row.comments}</TableCell>
                  <TableCell>
                    {row.recordedBy}
                    <div className="muted small">{formatDate(row.recordedAt)}</div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
