"use client";

import { useState, type FormEvent } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { Input } from "@/components/shadcn/input";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { ProfileView } from "@/lib/types";

type Props = {
  profile: ProfileView;
  onSaved: (profile: ProfileView) => void;
};

type Form = {
  bankAccountHolder: string;
  bankName: string;
  bankAccountNumber: string;
  bankIfsc: string;
  pan: string;
  uan: string;
};

function toForm(p: ProfileView): Form {
  const s = p.sensitive;
  return {
    bankAccountHolder: s?.bankAccountHolder ?? "",
    bankName: s?.bankName ?? "",
    bankAccountNumber: s?.bankAccountNumber ?? "",
    bankIfsc: s?.bankIfsc ?? "",
    pan: s?.pan ?? "",
    uan: s?.uan ?? "",
  };
}

function orNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}

export default function BankTaxCard({ profile, onSaved }: Props) {
  const [form, setForm] = useState<Form>(() => toForm(profile));
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const editable = profile.canEditSensitive;
  const masked = profile.sensitive?.masked ?? false;
  const set = (patch: Partial<Form>) => setForm((current) => ({ ...current, ...patch }));

  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      const next = await api<ProfileView>(`/profiles/${profile.userId}/sensitive`, {
        method: "PUT",
        body: {
          bankAccountHolder: orNull(form.bankAccountHolder),
          bankName: orNull(form.bankName),
          bankAccountNumber: orNull(form.bankAccountNumber),
          bankIfsc: orNull(form.bankIfsc),
          pan: orNull(form.pan),
          uan: orNull(form.uan),
          version: profile.version,
        },
      });
      onSaved(next);
      setForm(toForm(next));
      setSaved(true);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card className="max-w-3xl p-5">
      <form className="grid gap-4" onSubmit={(event) => void save(event)}>
        <p className="muted m-0 text-sm">{masked ? t("profile.bank.maskedHint") : t("profile.bank.help")}</p>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label={t("profile.bank.holder")}>
            <Input
              disabled={!editable}
              maxLength={150}
              value={form.bankAccountHolder}
              onChange={(e) => set({ bankAccountHolder: e.target.value })}
            />
          </Field>
          <Field label={t("profile.bank.bankName")}>
            <Input
              disabled={!editable}
              maxLength={100}
              value={form.bankName}
              onChange={(e) => set({ bankName: e.target.value })}
            />
          </Field>
          <Field label={t("profile.bank.account")} hint={editable ? t("profile.bank.accountHint") : undefined}>
            <Input
              disabled={!editable}
              maxLength={18}
              inputMode="numeric"
              autoComplete="off"
              value={form.bankAccountNumber}
              onChange={(e) => set({ bankAccountNumber: e.target.value })}
            />
          </Field>
          <Field label={t("profile.bank.ifsc")}>
            <Input
              disabled={!editable}
              maxLength={11}
              autoComplete="off"
              value={form.bankIfsc}
              onChange={(e) => set({ bankIfsc: e.target.value.toUpperCase() })}
            />
          </Field>
          <Field label={t("profile.bank.pan")}>
            <Input
              disabled={!editable}
              maxLength={10}
              autoComplete="off"
              value={form.pan}
              onChange={(e) => set({ pan: e.target.value.toUpperCase() })}
            />
          </Field>
          <Field label={t("profile.bank.uan")} hint={editable ? t("profile.bank.uanHint") : undefined}>
            <Input
              disabled={!editable}
              maxLength={12}
              inputMode="numeric"
              autoComplete="off"
              value={form.uan}
              onChange={(e) => set({ uan: e.target.value })}
            />
          </Field>
        </div>
        <ErrorBanner error={error} />
        {saved && <Alert variant="success">{t("common.saved")}</Alert>}
        {editable && (
          <div>
            <Button type="submit" disabled={busy}>
              {t("common.save")}
            </Button>
          </div>
        )}
      </form>
    </Card>
  );
}
