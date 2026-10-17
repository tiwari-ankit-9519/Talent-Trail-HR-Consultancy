"use client";

import { useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { PlacementDetail } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Textarea } from "@/components/shadcn/textarea";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";
import { DateField } from "@/components/shadcn/date-picker";

type Props = {
  submissionId: string;
  placement: PlacementDetail | null;
  onSaved: (placement: PlacementDetail) => void;
  onCancel?: () => void;
};

function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export default function OfferForm({ submissionId, placement, onSaved, onCancel }: Props) {
  const [ctc, setCtc] = useState(placement ? String(placement.offeredCtc) : "");
  const [offerDate, setOfferDate] = useState(placement?.offerDate ?? today());
  const [joiningDate, setJoiningDate] = useState(placement?.expectedJoiningDate ?? "");
  const [notes, setNotes] = useState(placement?.notes ?? "");
  const [sendEmail, setSendEmail] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [localError, setLocalError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLocalError("");
    if (joiningDate && offerDate && joiningDate < offerDate) {
      setLocalError(t("errors.EXPECTED_DATE_BEFORE_OFFER"));
      return;
    }
    if (offerDate > today()) {
      setLocalError(t("errors.OFFER_DATE_FUTURE"));
      return;
    }
    setBusy(true);
    setError(null);
    const body = {
      offeredCtc: ctc === "" ? null : Number(ctc),
      offerDate,
      expectedJoiningDate: joiningDate || null,
      notes,
      sendCandidateEmail: !placement && sendEmail,
      version: placement?.version,
    };
    try {
      const result = placement
        ? await api<PlacementDetail>(`/placements/${placement.id}/offer`, { method: "PUT", body })
        : await api<PlacementDetail>(`/submissions/${submissionId}/placement`, { method: "POST", body });
      onSaved(result);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card asChild>
      <form className="form-card" onSubmit={(e) => void submit(e)}>
        <h2>{placement ? t("placements.editOffer") : t("placements.recordOffer")}</h2>
        <ErrorBanner error={error} />
        {localError && (
          <Alert variant="destructive">
            <p>{localError}</p>
          </Alert>
        )}
        <div className="form-grid">
          <Field label={t("placements.offeredCtc")} hint={t("placements.offeredCtcHint")}>
            <Input type="number" min={1} step="0.01" required value={ctc} onChange={(e) => setCtc(e.target.value)} />
          </Field>
          <Field label={t("placements.offerDate")}>
            <DateField required max={today()} value={offerDate} onChange={(e) => setOfferDate(e.target.value)} />
          </Field>
          <Field label={t("placements.expectedJoiningDate")}>
            <DateField required min={offerDate} value={joiningDate} onChange={(e) => setJoiningDate(e.target.value)} />
          </Field>
        </div>
        <Field label={t("placements.notes")}>
          <Textarea rows={2} maxLength={2000} value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
        {!placement && (
          <label className="check">
            <CheckboxField checked={sendEmail} onChange={(e) => setSendEmail(e.target.checked)} />
            {t("placements.sendCandidateEmail")}
          </label>
        )}
        <div className="actions">
          <Button type="submit" disabled={busy}>
            {t("common.save")}
          </Button>
          {onCancel && (
            <Button variant="outline" type="button" disabled={busy} onClick={onCancel}>
              {t("common.cancel")}
            </Button>
          )}
        </div>
      </form>
    </Card>
  );
}
