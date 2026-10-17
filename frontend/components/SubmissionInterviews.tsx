"use client";

import { useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t, tx } from "@/lib/messages";
import type { InterviewMode, InterviewRow, InterviewStatus, SubmissionDetail } from "@/lib/types";
import { confirmDialog } from "@/lib/confirm";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Textarea } from "@/components/shadcn/textarea";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";
import { DateTimeField } from "@/components/shadcn/datetime-picker";

type Props = {
  submission: SubmissionDetail;
  canManage: boolean;
  onChanged: () => void;
};

const modes: InterviewMode[] = ["VIDEO", "PHONE", "IN_PERSON"];

function toLocalInput(iso: string): string {
  const date = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export default function SubmissionInterviews({ submission, canManage, onChanged }: Props) {
  const closed = ["SELECTED", "REJECTED", "WITHDRAWN"].includes(submission.status);
  const [editing, setEditing] = useState<InterviewRow | "new" | null>(null);
  const [scheduledAt, setScheduledAt] = useState("");
  const [duration, setDuration] = useState(60);
  const [mode, setMode] = useState<InterviewMode>("VIDEO");
  const [location, setLocation] = useState("");
  const [interviewer, setInterviewer] = useState("");
  const [notes, setNotes] = useState("");
  const [sendInvite, setSendInvite] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [localError, setLocalError] = useState("");

  function open(target: InterviewRow | "new") {
    setError(null);
    setLocalError("");
    setEditing(target);
    if (target === "new") {
      setScheduledAt("");
      setDuration(60);
      setMode("VIDEO");
      setLocation("");
      setInterviewer("");
      setNotes("");
      setSendInvite(true);
    } else {
      setScheduledAt(toLocalInput(target.scheduledAt));
      setDuration(target.durationMinutes);
      setMode(target.mode);
      setLocation(target.location ?? "");
      setInterviewer(target.interviewerName ?? "");
      setNotes(target.notes ?? "");
      setSendInvite(true);
    }
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!editing) return;
    setLocalError("");
    if (mode !== "PHONE" && !location.trim()) {
      setLocalError(t("errors.INTERVIEW_LOCATION_REQUIRED"));
      return;
    }
    if (scheduledAt && new Date(scheduledAt).getTime() < Date.now() - 5 * 60 * 1000) {
      setLocalError(t("errors.INTERVIEW_TIME_PAST"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const body = {
        scheduledAt: scheduledAt ? new Date(scheduledAt).toISOString() : null,
        durationMinutes: duration,
        mode,
        location,
        interviewerName: interviewer,
        notes,
        sendInvite,
        version: editing === "new" ? undefined : editing.version,
      };
      if (editing === "new") {
        await api(`/submissions/${submission.id}/interviews`, { method: "POST", body });
      } else {
        await api(`/submissions/${submission.id}/interviews/${editing.id}`, { method: "PUT", body });
      }
      setEditing(null);
      onChanged();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function setStatus(interview: InterviewRow, status: InterviewStatus) {
    if (status === "CANCELLED" && !(await confirmDialog(t("interviews.confirmCancel")))) return;
    setError(null);
    try {
      await api(`/submissions/${submission.id}/interviews/${interview.id}/status`, {
        method: "PATCH",
        body: { status, version: interview.version },
      });
      onChanged();
    } catch (failure) {
      setError(failure);
    }
  }

  async function resend(interview: InterviewRow) {
    setError(null);
    try {
      await api(`/submissions/${submission.id}/interviews/${interview.id}/resend`, { method: "POST" });
      onChanged();
    } catch (failure) {
      setError(failure);
    }
  }

  return (
    <div className="section-block">
      <div className="page-head">
        <h2>{t("interviews.heading")}</h2>
        {canManage && !closed && !editing && (
          <Button type="button" onClick={() => open("new")}>
            {t("interviews.schedule")}
          </Button>
        )}
      </div>
      {!editing && <ErrorBanner error={error} />}

      {editing && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void save(e)}>
            <h3>{editing === "new" ? t("interviews.schedule") : t("interviews.edit")}</h3>
            <ErrorBanner error={error} />
            {localError && (
              <Alert variant="destructive">
                <p>{localError}</p>
              </Alert>
            )}
            <div className="form-grid">
              <Field label={t("interviews.when")}>
                <DateTimeField required value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)} />
              </Field>
              <Field label={t("interviews.duration")}>
                <Input
                  type="number"
                  min={15}
                  max={480}
                  step={15}
                  value={duration}
                  onChange={(e) => setDuration(Number(e.target.value))}
                />
              </Field>
              <Field label={t("interviews.mode")}>
                <FormSelect value={mode} onChange={(e) => setMode(e.target.value as InterviewMode)}>
                  {modes.map((value) => (
                    <option key={value} value={value}>
                      {tx(`enums.interviewMode.${value}`)}
                    </option>
                  ))}
                </FormSelect>
              </Field>
              <Field label={t(`interviews.location_${mode}`)}>
                <Input value={location} maxLength={500} onChange={(e) => setLocation(e.target.value)} />
              </Field>
              <Field label={t("interviews.interviewer")}>
                <Input value={interviewer} maxLength={150} onChange={(e) => setInterviewer(e.target.value)} />
              </Field>
            </div>
            <Field label={t("interviews.notes")}>
              <Textarea rows={2} maxLength={2000} value={notes} onChange={(e) => setNotes(e.target.value)} />
            </Field>
            <label className="check">
              <CheckboxField checked={sendInvite} onChange={(e) => setSendInvite(e.target.checked)} />
              {t("interviews.sendInvite")}
            </label>
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("common.save")}
              </Button>
              <Button variant="outline" type="button" disabled={busy} onClick={() => setEditing(null)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {submission.interviews.length === 0 ? (
        <p className="muted">{t("interviews.none")}</p>
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("interviews.round")}</TableHead>
                <TableHead>{t("interviews.when")}</TableHead>
                <TableHead>{t("interviews.mode")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("interviews.invite")}</TableHead>
                {canManage && <TableHead>{t("common.actions")}</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {submission.interviews.map((interview) => (
                <TableRow key={interview.id}>
                  <TableCell>{interview.roundNumber}</TableCell>
                  <TableCell>
                    {formatDate(interview.scheduledAt)}
                    <div className="muted small">
                      {interview.durationMinutes} {t("interviews.minutes")}
                      {interview.location ? ` · ${interview.location}` : ""}
                      {interview.interviewerName ? ` · ${interview.interviewerName}` : ""}
                    </div>
                  </TableCell>
                  <TableCell>{tx(`enums.interviewMode.${interview.mode}`)}</TableCell>
                  <TableCell>
                    <StatusPill kind="interviewStatus" value={interview.status} />
                  </TableCell>
                  <TableCell>
                    <StatusPill kind="emailStatus" value={interview.inviteStatus} />
                    {interview.inviteError && <div className="muted small">{interview.inviteError}</div>}
                  </TableCell>
                  {canManage && (
                    <TableCell>
                      {interview.status === "SCHEDULED" && (
                        <div className="row-actions">
                          <Button variant="outline" type="button" onClick={() => open(interview)}>
                            {t("common.edit")}
                          </Button>
                          <Button
                            variant="outline"
                            type="button"
                            onClick={() => void setStatus(interview, "COMPLETED")}
                          >
                            {t("interviews.markCompleted")}
                          </Button>
                          <Button variant="outline" type="button" onClick={() => void setStatus(interview, "NO_SHOW")}>
                            {t("interviews.markNoShow")}
                          </Button>
                          <Button
                            variant="outline"
                            type="button"
                            onClick={() => void setStatus(interview, "CANCELLED")}
                          >
                            {t("interviews.cancel")}
                          </Button>
                          {interview.inviteStatus !== "SENT" && interview.inviteStatus !== "QUEUED" && (
                            <Button variant="outline" type="button" onClick={() => void resend(interview)}>
                              {t("interviews.sendNow")}
                            </Button>
                          )}
                        </div>
                      )}
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
