"use client";

import { useEffect, useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { ClientSummary, JobDetail, Page } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Textarea } from "@/components/shadcn/textarea";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";
import { DateField } from "@/components/shadcn/date-picker";

type Props = {
  job: JobDetail | null;
  canManage: boolean;
  onSaved: (job: JobDetail) => void;
};

type Form = {
  clientId: string;
  title: string;
  description: string;
  location: string;
  skills: string;
  minExperienceYears: string;
  maxExperienceYears: string;
  minCtc: string;
  maxCtc: string;
  openings: string;
  published: boolean;
  closingDate: string;
};

function text(value: number | null | undefined): string {
  return value === null || value === undefined ? "" : String(value);
}

function toForm(job: JobDetail | null): Form {
  return {
    clientId: job?.clientId ?? "",
    title: job?.title ?? "",
    description: job?.description ?? "",
    location: job?.location ?? "",
    skills: job?.skills ?? "",
    minExperienceYears: text(job?.minExperienceYears),
    maxExperienceYears: text(job?.maxExperienceYears),
    minCtc: text(job?.minCtc),
    maxCtc: text(job?.maxCtc),
    openings: job ? String(job.openings) : "1",
    published: job?.published ?? false,
    closingDate: job?.closingDate ?? "",
  };
}

function numberOrNull(value: string): number | null {
  return value.trim() === "" ? null : Number(value);
}

export default function JobForm({ job, canManage, onSaved }: Props) {
  const [form, setForm] = useState<Form>(toForm(job));
  const [clients, setClients] = useState<ClientSummary[]>([]);
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api<Page<ClientSummary>>("/clients", { query: { status: "ACTIVE", size: 100 } })
      .then((result) => setClients(result.items))
      .catch(setError);
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      const body = {
        clientId: form.clientId,
        title: form.title,
        description: form.description,
        location: form.location,
        skills: form.skills,
        minExperienceYears: numberOrNull(form.minExperienceYears),
        maxExperienceYears: numberOrNull(form.maxExperienceYears),
        minCtc: numberOrNull(form.minCtc),
        maxCtc: numberOrNull(form.maxCtc),
        openings: Number(form.openings),
        published: form.published,
        closingDate: form.closingDate === "" ? null : form.closingDate,
        version: job?.version ?? null,
      };
      const result = job
        ? await api<JobDetail>(`/jobs/${job.id}`, { method: "PUT", body })
        : await api<JobDetail>("/jobs", { method: "POST", body });
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
          <Field label={t("jobs.client")}>
            <FormSelect
              required
              disabled={disabled}
              value={form.clientId}
              onChange={(e) => setForm({ ...form, clientId: e.target.value })}
            >
              <option value="">{t("jobs.selectClient")}</option>
              {job && !clients.some((c) => c.id === job.clientId) && (
                <option value={job.clientId}>{job.clientName}</option>
              )}
              {clients.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </FormSelect>
          </Field>
          <Field label={t("jobs.title")}>
            <Input
              required
              disabled={disabled}
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
            />
          </Field>
          <Field label={t("jobs.location")}>
            <Input
              disabled={disabled}
              value={form.location}
              onChange={(e) => setForm({ ...form, location: e.target.value })}
            />
          </Field>
          <Field label={t("jobs.openings")}>
            <Input
              type="number"
              min="1"
              max="1000"
              required
              disabled={disabled}
              value={form.openings}
              onChange={(e) => setForm({ ...form, openings: e.target.value })}
            />
          </Field>
          <Field label={t("jobs.minExperience")}>
            <Input
              type="number"
              step="0.5"
              min="0"
              disabled={disabled}
              value={form.minExperienceYears}
              onChange={(e) => setForm({ ...form, minExperienceYears: e.target.value })}
            />
          </Field>
          <Field label={t("jobs.maxExperience")}>
            <Input
              type="number"
              step="0.5"
              min="0"
              disabled={disabled}
              value={form.maxExperienceYears}
              onChange={(e) => setForm({ ...form, maxExperienceYears: e.target.value })}
            />
          </Field>
          <Field label={t("jobs.minCtc")}>
            <Input
              type="number"
              min="0"
              disabled={disabled}
              value={form.minCtc}
              onChange={(e) => setForm({ ...form, minCtc: e.target.value })}
            />
          </Field>
          <Field label={t("jobs.maxCtc")}>
            <Input
              type="number"
              min="0"
              disabled={disabled}
              value={form.maxCtc}
              onChange={(e) => setForm({ ...form, maxCtc: e.target.value })}
            />
          </Field>
          <Field label={t("jobs.closingDate")}>
            <DateField
              disabled={disabled}
              value={form.closingDate}
              onChange={(e) => setForm({ ...form, closingDate: e.target.value })}
            />
          </Field>
        </div>
        <Field label={t("jobs.skills")} hint={t("jobs.skillsHint")}>
          <Input
            disabled={disabled}
            value={form.skills}
            onChange={(e) => setForm({ ...form, skills: e.target.value })}
          />
        </Field>
        <Field label={t("jobs.description")} hint={t("jobs.descriptionHint")}>
          <Textarea
            rows={8}
            disabled={disabled}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </Field>
        <label className="check">
          <CheckboxField
            disabled={disabled}
            checked={form.published}
            onChange={(e) => setForm({ ...form, published: e.target.checked })}
          />
          {t("jobs.publishOnApplyPage")}
        </label>
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
