"use client";

import { useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { ClientDetail } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { Input } from "@/components/shadcn/input";
import { Textarea } from "@/components/shadcn/textarea";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";

type Props = {
  client: ClientDetail | null;
  canManage: boolean;
  onSaved: (client: ClientDetail) => void;
};

type Form = {
  name: string;
  legalName: string;
  industry: string;
  website: string;
  gstin: string;
  pan: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  notes: string;
};

function toForm(client: ClientDetail | null): Form {
  return {
    name: client?.name ?? "",
    legalName: client?.legalName ?? "",
    industry: client?.industry ?? "",
    website: client?.website ?? "",
    gstin: client?.gstin ?? "",
    pan: client?.pan ?? "",
    addressLine1: client?.addressLine1 ?? "",
    addressLine2: client?.addressLine2 ?? "",
    city: client?.city ?? "",
    state: client?.state ?? "",
    postalCode: client?.postalCode ?? "",
    country: client?.country ?? "India",
    notes: client?.notes ?? "",
  };
}

const textFields: { key: keyof Form; label: Parameters<typeof t>[0] }[] = [
  { key: "name", label: "clients.name" },
  { key: "legalName", label: "clients.legalName" },
  { key: "industry", label: "clients.industry" },
  { key: "website", label: "clients.website" },
  { key: "gstin", label: "clients.gstin" },
  { key: "pan", label: "clients.pan" },
  { key: "addressLine1", label: "clients.addressLine1" },
  { key: "addressLine2", label: "clients.addressLine2" },
  { key: "city", label: "clients.city" },
  { key: "state", label: "clients.state" },
  { key: "postalCode", label: "clients.postalCode" },
  { key: "country", label: "clients.country" },
];

export default function ClientProfileForm({ client, canManage, onSaved }: Props) {
  const [form, setForm] = useState<Form>(toForm(client));
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [saved, setSaved] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      const body = { ...form, version: client?.version ?? null };
      const result = client
        ? await api<ClientDetail>(`/clients/${client.id}`, { method: "PUT", body })
        : await api<ClientDetail>("/clients", { method: "POST", body });
      setForm(toForm(result));
      setSaved(true);
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
        <ErrorBanner error={error} />
        {saved && <Alert variant="success">{t("common.saved")}</Alert>}
        <div className="form-grid">
          {textFields.map(({ key, label }) => (
            <Field key={key} label={t(label)}>
              <Input
                required={key === "name"}
                disabled={!canManage}
                value={form[key]}
                onChange={(e) => setForm({ ...form, [key]: e.target.value })}
              />
            </Field>
          ))}
        </div>
        <Field label={t("clients.notes")}>
          <Textarea
            rows={3}
            disabled={!canManage}
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
