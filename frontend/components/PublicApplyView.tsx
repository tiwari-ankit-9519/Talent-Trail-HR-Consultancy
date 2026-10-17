"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";
import { experienceRange } from "@/components/PublicJobsView";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { publicApi } from "@/lib/api";
import { t } from "@/lib/messages";
import type { PublicJob } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";

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
  skills: string;
  website: string;
};

const empty: Form = {
  fullName: "",
  email: "",
  phone: "",
  currentCompany: "",
  currentTitle: "",
  totalExperienceYears: "",
  currentCtc: "",
  expectedCtc: "",
  noticePeriodDays: "",
  currentLocation: "",
  skills: "",
  website: "",
};

export default function PublicApplyView({ code }: { code: string }) {
  const [job, setJob] = useState<PublicJob | null>(null);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [form, setForm] = useState<Form>(empty);
  const [resume, setResume] = useState<File | null>(null);
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  useEffect(() => {
    publicApi<PublicJob>(`/jobs/${encodeURIComponent(code)}`)
      .then(setJob)
      .catch(setLoadError);
  }, [code]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const data = new FormData();
      Object.entries(form).forEach(([key, value]) => data.append(key, value));
      data.append("consent", String(consent));
      if (resume) data.append("resume", resume);
      await publicApi(`/jobs/${encodeURIComponent(code)}/apply`, data);
      setDone(true);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  function set(key: keyof Form) {
    return (event: { target: { value: string } }) => setForm({ ...form, [key]: event.target.value });
  }

  if (loadError) {
    return (
      <section>
        <p>
          <Link href="/apply">{t("apply.backToRoles")}</Link>
        </p>
        <Alert variant="destructive">{t("apply.notAvailable")}</Alert>
      </section>
    );
  }

  if (!job) {
    return <Loading />;
  }

  if (done) {
    return (
      <Card className="grid gap-3 p-5">
        <h1>{t("apply.thanksHeading")}</h1>
        <p>{t("apply.thanksBody")}</p>
        <p>
          <Link href="/apply">{t("apply.backToRoles")}</Link>
        </p>
      </Card>
    );
  }

  return (
    <section>
      <p>
        <Link href="/apply">{t("apply.backToRoles")}</Link>
      </p>
      <h1>{job.title}</h1>
      <p className="muted">{[job.location, experienceRange(job)].filter(Boolean).join(" · ")}</p>
      {job.skills && (
        <p>
          <strong>{t("apply.skills")}:</strong> {job.skills}
        </p>
      )}
      {job.description && <p className="job-description">{job.description}</p>}

      <Card asChild>
        <form className="form-card" onSubmit={(e) => void submit(e)}>
          <h2>{t("apply.formHeading")}</h2>
          <ErrorBanner error={error} />
          <div className="form-grid">
            <Field label={t("apply.fullName")}>
              <Input required maxLength={150} value={form.fullName} onChange={set("fullName")} />
            </Field>
            <Field label={t("apply.email")}>
              <Input type="email" required maxLength={255} value={form.email} onChange={set("email")} />
            </Field>
            <Field label={t("apply.phone")}>
              <Input maxLength={30} value={form.phone} onChange={set("phone")} />
            </Field>
            <Field label={t("apply.currentLocation")}>
              <Input maxLength={150} value={form.currentLocation} onChange={set("currentLocation")} />
            </Field>
            <Field label={t("apply.currentCompany")}>
              <Input maxLength={200} value={form.currentCompany} onChange={set("currentCompany")} />
            </Field>
            <Field label={t("apply.currentTitle")}>
              <Input maxLength={200} value={form.currentTitle} onChange={set("currentTitle")} />
            </Field>
            <Field label={t("apply.experience")}>
              <Input
                type="number"
                step="0.5"
                min="0"
                value={form.totalExperienceYears}
                onChange={set("totalExperienceYears")}
              />
            </Field>
            <Field label={t("apply.noticePeriod")}>
              <Input type="number" min="0" max="365" value={form.noticePeriodDays} onChange={set("noticePeriodDays")} />
            </Field>
            <Field label={t("apply.currentCtc")}>
              <Input type="number" min="0" value={form.currentCtc} onChange={set("currentCtc")} />
            </Field>
            <Field label={t("apply.expectedCtc")}>
              <Input type="number" min="0" value={form.expectedCtc} onChange={set("expectedCtc")} />
            </Field>
          </div>
          <Field label={t("apply.skillsField")}>
            <Input maxLength={2000} value={form.skills} onChange={set("skills")} />
          </Field>
          <Field label={t("apply.resume")} hint={t("apply.resumeHint")}>
            <Input
              type="file"
              required
              accept=".pdf,.doc,.docx"
              onChange={(e) => setResume(e.target.files?.[0] ?? null)}
            />
          </Field>
          <div className="honeypot" aria-hidden="true">
            <label>
              {t("apply.honeypot")}
              <Input tabIndex={-1} autoComplete="off" value={form.website} onChange={set("website")} />
            </label>
          </div>
          <label className="check">
            <CheckboxField required checked={consent} onChange={(e) => setConsent(e.target.checked)} />
            {t("apply.consent")}
          </label>
          <div className="actions">
            <Button type="submit" disabled={busy || !resume || !consent}>
              {busy ? t("apply.submitting") : t("apply.submit")}
            </Button>
          </div>
        </form>
      </Card>
    </section>
  );
}
