"use client";

import { useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { t, tx } from "@/lib/messages";
import type { CandidateDetail, CandidateSource } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Textarea } from "@/components/shadcn/textarea";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";

type Props = {
  candidate: CandidateDetail | null;
  canManage: boolean;
  onSaved: (candidate: CandidateDetail) => void;
};

type Form = {
  fullName: string;
  email: string;
  phone: string;
  currentCompany: string;
  currentTitle: string;
  totalExperienceYears: string;
  currentCtc: string;
  expectedCtc: string;
  noticePeriodDays: string;
  currentLocation: string;
  preferredLocation: string;
  skills: string;
  source: CandidateSource;
  notes: string;
};

const sources: CandidateSource[] = ["LINKEDIN", "NAUKRI", "APPLY_LINK", "REFERRAL", "OTHER"];

function text(value: number | null | undefined): string {
  return value === null || value === undefined ? "" : String(value);
}

function toForm(candidate: CandidateDetail | null): Form {
  return {
    fullName: candidate?.fullName ?? "",
    email: candidate?.email ?? "",
    phone: candidate?.phone ?? "",
    currentCompany: candidate?.currentCompany ?? "",
    currentTitle: candidate?.currentTitle ?? "",
    totalExperienceYears: text(candidate?.totalExperienceYears),
    currentCtc: text(candidate?.currentCtc),
    expectedCtc: text(candidate?.expectedCtc),
    noticePeriodDays: text(candidate?.noticePeriodDays),
    currentLocation: candidate?.currentLocation ?? "",
    preferredLocation: candidate?.preferredLocation ?? "",
    skills: candidate?.skills ?? "",
    source: candidate?.source ?? "LINKEDIN",
    notes: candidate?.notes ?? "",
  };
}

function numberOrNull(value: string): number | null {
  return value.trim() === "" ? null : Number(value);
}

export default function CandidateForm({ candidate, canManage, onSaved }: Props) {
  const [form, setForm] = useState<Form>(toForm(candidate));
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      const body = {
        ...form,
        totalExperienceYears: numberOrNull(form.totalExperienceYears),
        currentCtc: numberOrNull(form.currentCtc),
        expectedCtc: numberOrNull(form.expectedCtc),
        noticePeriodDays: numberOrNull(form.noticePeriodDays),
        version: candidate?.version ?? null,
      };
      const result = candidate
        ? await api<CandidateDetail>(`/candidates/${candidate.id}`, { method: "PUT", body })
        : await api<CandidateDetail>("/candidates", { method: "POST", body });
      setForm(toForm(result));
      setSaved(true);
      onSaved(result);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  const disabled = !canManage;

  return (
    <Card asChild>
      <form className="form-card" onSubmit={(e) => void submit(e)}>
        <ErrorBanner error={error} />
        {saved && <Alert variant="success">{t("common.saved")}</Alert>}
        <div className="form-grid">
          <Field label={t("candidates.fullName")}>
            <Input
              required
              disabled={disabled}
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.email")}>
            <Input
              type="email"
              required
              disabled={disabled}
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.phone")}>
            <Input
              disabled={disabled}
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.source")}>
            <FormSelect
              disabled={disabled}
              value={form.source}
              onChange={(e) => setForm({ ...form, source: e.target.value as CandidateSource })}
            >
              {sources.map((value) => (
                <option key={value} value={value}>
                  {tx(`enums.candidateSource.${value}`)}
                </option>
              ))}
            </FormSelect>
          </Field>
          <Field label={t("candidates.currentCompany")}>
            <Input
              disabled={disabled}
              value={form.currentCompany}
              onChange={(e) => setForm({ ...form, currentCompany: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.currentTitle")}>
            <Input
              disabled={disabled}
              value={form.currentTitle}
              onChange={(e) => setForm({ ...form, currentTitle: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.experience")}>
            <Input
              type="number"
              step="0.5"
              min="0"
              disabled={disabled}
              value={form.totalExperienceYears}
              onChange={(e) => setForm({ ...form, totalExperienceYears: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.noticePeriod")}>
            <Input
              type="number"
              min="0"
              max="365"
              disabled={disabled}
              value={form.noticePeriodDays}
              onChange={(e) => setForm({ ...form, noticePeriodDays: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.currentCtc")}>
            <Input
              type="number"
              min="0"
              disabled={disabled}
              value={form.currentCtc}
              onChange={(e) => setForm({ ...form, currentCtc: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.expectedCtc")}>
            <Input
              type="number"
              min="0"
              disabled={disabled}
              value={form.expectedCtc}
              onChange={(e) => setForm({ ...form, expectedCtc: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.currentLocation")}>
            <Input
              disabled={disabled}
              value={form.currentLocation}
              onChange={(e) => setForm({ ...form, currentLocation: e.target.value })}
            />
          </Field>
          <Field label={t("candidates.preferredLocation")}>
            <Input
              disabled={disabled}
              value={form.preferredLocation}
              onChange={(e) => setForm({ ...form, preferredLocation: e.target.value })}
            />
          </Field>
        </div>
        <Field label={t("candidates.skills")} hint={t("candidates.skillsHint")}>
          <Input
            disabled={disabled}
            value={form.skills}
            onChange={(e) => setForm({ ...form, skills: e.target.value })}
          />
        </Field>
        <Field label={t("candidates.notes")}>
          <Textarea
            rows={4}
            disabled={disabled}
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
        </Field>
        {canManage && (
          <div className="actions">
            <Button type="submit" disabled={busy}>
              {t("common.save")}
            </Button>
          </div>
        )}
      </form>
    </Card>
  );
}
