"use client";

import { useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t, tx } from "@/lib/messages";
import type { CandidateDetail, ConsentStatus } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Card } from "@/components/shadcn/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { badgeVariants } from "@/components/shadcn/badge";
import StatusPill from "@/components/StatusPill";

type Props = {
  candidate: CandidateDetail;
  canManage: boolean;
  onChanged: (candidate: CandidateDetail) => void;
};

const choices: ConsentStatus[] = ["GRANTED", "DENIED", "WITHDRAWN"];

export default function CandidateConsent({ candidate, canManage, onChanged }: Props) {
  const [status, setStatus] = useState<ConsentStatus>("GRANTED");
  const [method, setMethod] = useState("CALL");
  const [note, setNote] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await api<CandidateDetail>(`/candidates/${candidate.id}/consent`, {
        method: "POST",
        body: { status, method, note },
      });
      setNote("");
      onChanged(result);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="section-block">
      <Card asChild>
        <div className="form-card">
          <h2>{t("consent.current")}</h2>
          <p>
            <span
              className={
                candidate.consentStatus === "GRANTED"
                  ? badgeVariants({ variant: "ok" })
                  : badgeVariants({ variant: "warn" })
              }
            >
              {tx(`enums.consentStatus.${candidate.consentStatus}`)}
            </span>
            {candidate.consentMethod && (
              <span className="muted">
                {" "}
                {tx(`enums.consentMethod.${candidate.consentMethod}`)} · {formatDate(candidate.consentRecordedAt)}
              </span>
            )}
          </p>
          <p className="muted small">{t("consent.rule")}</p>
        </div>
      </Card>

      {canManage && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void submit(e)}>
            <h2>{t("consent.record")}</h2>
            <ErrorBanner error={error} />
            <div className="form-grid">
              <Field label={t("consent.decision")}>
                <FormSelect value={status} onChange={(e) => setStatus(e.target.value as ConsentStatus)}>
                  {choices.map((value) => (
                    <option key={value} value={value}>
                      {tx(`enums.consentStatus.${value}`)}
                    </option>
                  ))}
                </FormSelect>
              </Field>
              <Field label={t("consent.method")}>
                <FormSelect value={method} onChange={(e) => setMethod(e.target.value)}>
                  <option value="CALL">{tx("enums.consentMethod.CALL")}</option>
                  <option value="EMAIL">{tx("enums.consentMethod.EMAIL")}</option>
                </FormSelect>
              </Field>
            </div>
            <Field label={t("consent.note")} hint={t("consent.noteHint")}>
              <Input value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} />
            </Field>
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("consent.save")}
              </Button>
            </div>
          </form>
        </Card>
      )}

      <h2>{t("consent.history")}</h2>
      {candidate.consentHistory.length === 0 ? (
        <p className="muted">{t("consent.none")}</p>
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("audit.when")}</TableHead>
                <TableHead>{t("consent.decision")}</TableHead>
                <TableHead>{t("consent.method")}</TableHead>
                <TableHead>{t("consent.note")}</TableHead>
                <TableHead>{t("consent.recordedBy")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {candidate.consentHistory.map((event) => (
                <TableRow key={`${event.recordedAt}-${event.status}`}>
                  <TableCell>{formatDate(event.recordedAt)}</TableCell>
                  <TableCell>
                    <StatusPill kind="consentStatus" value={event.status} />
                  </TableCell>
                  <TableCell>{tx(`enums.consentMethod.${event.method}`)}</TableCell>
                  <TableCell>{event.note}</TableCell>
                  <TableCell>{event.recordedBy}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
