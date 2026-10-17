"use client";

import { Badge } from "@/components/shadcn/badge";
import { useEffect, useState, type FormEvent } from "react";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { CompanyProfile } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { Input } from "@/components/shadcn/input";
import { Card } from "@/components/shadcn/card";

type Form = Record<Exclude<keyof CompanyProfile, "version">, string>;

function toForm(profile: CompanyProfile): Form {
  return {
    legalName: profile.legalName,
    addressLine1: profile.addressLine1 ?? "",
    addressLine2: profile.addressLine2 ?? "",
    city: profile.city ?? "",
    state: profile.state ?? "",
    postalCode: profile.postalCode ?? "",
    gstin: profile.gstin ?? "",
    pan: profile.pan ?? "",
    email: profile.email ?? "",
    phone: profile.phone ?? "",
    sacCode: profile.sacCode,
    invoicePrefix: profile.invoicePrefix,
    bankAccountName: profile.bankAccountName ?? "",
    bankName: profile.bankName ?? "",
    bankAccountNumber: profile.bankAccountNumber ?? "",
    bankIfsc: profile.bankIfsc ?? "",
    bankBranch: profile.bankBranch ?? "",
    invoiceFooter: profile.invoiceFooter ?? "",
  };
}

export default function CompanySettingsView() {
  const [version, setVersion] = useState<number | null>(null);
  const [form, setForm] = useState<Form | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void (async () => {
      try {
        const profile = await api<CompanyProfile>("/settings/company");
        setForm(toForm(profile));
        setVersion(profile.version);
      } catch (failure) {
        setError(failure);
      }
    })();
  }, []);

  if (!form) {
    return (
      <section>
        <ErrorBanner error={error} />
        {!error && <Loading />}
      </section>
    );
  }

  function bind(name: keyof Form) {
    return {
      value: form ? form[name] : "",
      onChange: (e: { target: { value: string } }) =>
        setForm((current) => (current ? { ...current, [name]: e.target.value } : current)),
    };
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      const profile = await api<CompanyProfile>("/settings/company", {
        method: "PUT",
        body: { ...form, version },
      });
      setForm(toForm(profile));
      setVersion(profile.version);
      setSaved(true);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <h1>{t("company.heading")}</h1>
      <p className="muted">{t("company.help")}</p>
      <Card asChild>
        <form className="form-card" onSubmit={(e) => void submit(e)}>
          <ErrorBanner error={error} />
          {saved && <Badge variant="ok">{t("common.saved")}</Badge>}
          <h2>{t("company.identity")}</h2>
          <div className="form-grid">
            <Field label={t("company.legalName")}>
              <Input required maxLength={250} {...bind("legalName")} />
            </Field>
            <Field label={t("company.gstin")}>
              <Input maxLength={15} {...bind("gstin")} />
            </Field>
            <Field label={t("company.pan")}>
              <Input maxLength={10} {...bind("pan")} />
            </Field>
            <Field label={t("company.email")} hint={t("company.emailHint")}>
              <Input type="email" maxLength={255} {...bind("email")} />
            </Field>
            <Field label={t("company.phone")}>
              <Input maxLength={30} {...bind("phone")} />
            </Field>
          </div>
          <h2>{t("company.address")}</h2>
          <div className="form-grid">
            <Field label={t("company.addressLine1")}>
              <Input maxLength={200} {...bind("addressLine1")} />
            </Field>
            <Field label={t("company.addressLine2")}>
              <Input maxLength={200} {...bind("addressLine2")} />
            </Field>
            <Field label={t("company.city")}>
              <Input maxLength={100} {...bind("city")} />
            </Field>
            <Field label={t("company.state")} hint={t("company.stateHint")}>
              <Input maxLength={100} {...bind("state")} />
            </Field>
            <Field label={t("company.postalCode")}>
              <Input maxLength={12} {...bind("postalCode")} />
            </Field>
          </div>
          <h2>{t("company.bank")}</h2>
          <div className="form-grid">
            <Field label={t("company.bankAccountName")}>
              <Input maxLength={200} {...bind("bankAccountName")} />
            </Field>
            <Field label={t("company.bankName")}>
              <Input maxLength={200} {...bind("bankName")} />
            </Field>
            <Field label={t("company.bankAccountNumber")}>
              <Input maxLength={40} {...bind("bankAccountNumber")} />
            </Field>
            <Field label={t("company.bankIfsc")}>
              <Input maxLength={15} {...bind("bankIfsc")} />
            </Field>
            <Field label={t("company.bankBranch")}>
              <Input maxLength={200} {...bind("bankBranch")} />
            </Field>
          </div>
          <h2>{t("company.invoiceSection")}</h2>
          <div className="form-grid">
            <Field label={t("company.invoicePrefix")} hint={t("company.invoicePrefixHint")}>
              <Input required maxLength={10} {...bind("invoicePrefix")} />
            </Field>
            <Field label={t("company.sacCode")} hint={t("company.sacCodeHint")}>
              <Input required maxLength={10} {...bind("sacCode")} />
            </Field>
          </div>
          <div className="actions">
            <Button type="submit" disabled={busy}>
              {t("common.save")}
            </Button>
          </div>
        </form>
      </Card>
    </section>
  );
}
