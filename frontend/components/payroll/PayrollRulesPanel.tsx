"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { Checkbox } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { PayrollSettings } from "@/lib/types";

type Form = {
  pfEnabled: boolean;
  pfRate: string;
  pfWageCeiling: string;
  ptAmount: string;
  ptMinGross: string;
  basicPercent: string;
  hraPercent: string;
  employerPfRate: string;
  gratuityEnabled: boolean;
  gratuityPercent: string;
};

function toForm(s: PayrollSettings): Form {
  return {
    pfEnabled: s.pfEnabled,
    pfRate: String(s.pfRate),
    pfWageCeiling: String(s.pfWageCeiling),
    ptAmount: String(s.ptAmount),
    ptMinGross: String(s.ptMinGross),
    basicPercent: String(s.basicPercent),
    hraPercent: String(s.hraPercent),
    employerPfRate: String(s.employerPfRate),
    gratuityEnabled: s.gratuityEnabled,
    gratuityPercent: String(s.gratuityPercent),
  };
}

export default function PayrollRulesPanel() {
  const [settings, setSettings] = useState<PayrollSettings | null>(null);
  const [form, setForm] = useState<Form | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api<PayrollSettings>("/payroll/settings")
      .then((value) => {
        setSettings(value);
        setForm(toForm(value));
      })
      .catch(setError);
  }, []);

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!form || !settings) return;
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      const next = await api<PayrollSettings>("/payroll/settings", {
        method: "PUT",
        body: {
          pfEnabled: form.pfEnabled,
          pfRate: Number(form.pfRate),
          pfWageCeiling: Number(form.pfWageCeiling),
          ptAmount: Number(form.ptAmount),
          ptMinGross: Number(form.ptMinGross),
          basicPercent: Number(form.basicPercent),
          hraPercent: Number(form.hraPercent),
          employerPfRate: Number(form.employerPfRate),
          gratuityEnabled: form.gratuityEnabled,
          gratuityPercent: Number(form.gratuityPercent),
          version: settings.version,
        },
      });
      setSettings(next);
      setForm(toForm(next));
      setSaved(true);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  if (!form) return error ? <ErrorBanner error={error} /> : <Loading />;

  return (
    <Card className="max-w-2xl p-5">
      <form className="grid gap-4" onSubmit={(event) => void save(event)}>
        <p className="muted m-0 text-sm">{t("payroll.rules.help")}</p>
        <label className="flex cursor-pointer items-center gap-2 text-sm">
          <Checkbox
            checked={form.pfEnabled}
            onCheckedChange={(checked) => setForm({ ...form, pfEnabled: checked === true })}
          />
          {t("payroll.rules.pfEnabled")}
        </label>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label={t("payroll.rules.pfRate")}>
            <Input
              required
              type="number"
              min={0}
              max={100}
              step="0.01"
              value={form.pfRate}
              onChange={(e) => setForm({ ...form, pfRate: e.target.value })}
            />
          </Field>
          <Field label={t("payroll.rules.pfCeiling")} hint={t("payroll.rules.pfCeilingHint")}>
            <Input
              required
              type="number"
              min={0}
              step="0.01"
              value={form.pfWageCeiling}
              onChange={(e) => setForm({ ...form, pfWageCeiling: e.target.value })}
            />
          </Field>
          <Field label={t("payroll.rules.ptAmount")}>
            <Input
              required
              type="number"
              min={0}
              step="0.01"
              value={form.ptAmount}
              onChange={(e) => setForm({ ...form, ptAmount: e.target.value })}
            />
          </Field>
          <Field label={t("payroll.rules.ptMinGross")} hint={t("payroll.rules.ptMinGrossHint")}>
            <Input
              required
              type="number"
              min={0}
              step="0.01"
              value={form.ptMinGross}
              onChange={(e) => setForm({ ...form, ptMinGross: e.target.value })}
            />
          </Field>
        </div>
        <h3 className="m-0 text-sm font-semibold">{t("payroll.rules.ctcHeading")}</h3>
        <p className="muted m-0 text-sm">{t("payroll.rules.ctcHelp")}</p>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label={t("payroll.rules.basicPercent")}>
            <Input
              required
              type="number"
              min={1}
              max={100}
              step="0.01"
              value={form.basicPercent}
              onChange={(e) => setForm({ ...form, basicPercent: e.target.value })}
            />
          </Field>
          <Field label={t("payroll.rules.hraPercent")}>
            <Input
              required
              type="number"
              min={0}
              max={100}
              step="0.01"
              value={form.hraPercent}
              onChange={(e) => setForm({ ...form, hraPercent: e.target.value })}
            />
          </Field>
          <Field label={t("payroll.rules.employerPfRate")}>
            <Input
              required
              type="number"
              min={0}
              max={100}
              step="0.01"
              value={form.employerPfRate}
              onChange={(e) => setForm({ ...form, employerPfRate: e.target.value })}
            />
          </Field>
          <Field label={t("payroll.rules.gratuityPercent")}>
            <Input
              required
              type="number"
              min={0}
              max={100}
              step="0.01"
              value={form.gratuityPercent}
              onChange={(e) => setForm({ ...form, gratuityPercent: e.target.value })}
            />
          </Field>
        </div>
        <label className="flex cursor-pointer items-center gap-2 text-sm">
          <Checkbox
            checked={form.gratuityEnabled}
            onCheckedChange={(checked) => setForm({ ...form, gratuityEnabled: checked === true })}
          />
          {t("payroll.rules.gratuityEnabled")}
        </label>
        <ErrorBanner error={error} />
        {saved && <Alert variant="success">{t("common.saved")}</Alert>}
        <div>
          <Button type="submit" disabled={busy}>
            {t("common.save")}
          </Button>
        </div>
      </form>
    </Card>
  );
}
