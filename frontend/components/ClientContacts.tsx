"use client";

import { useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { ClientContact } from "@/lib/types";
import { confirmDialog } from "@/lib/confirm";
import { Button } from "@/components/shadcn/button";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Badge } from "@/components/shadcn/badge";
import { Card } from "@/components/shadcn/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";

type Props = {
  clientId: string;
  contacts: ClientContact[];
  canManage: boolean;
  onChanged: () => void;
};

type Draft = {
  id: string | null;
  name: string;
  email: string;
  phone: string;
  designation: string;
  primaryContact: boolean;
};

const blank: Draft = { id: null, name: "", email: "", phone: "", designation: "", primaryContact: false };

export default function ClientContacts({ clientId, contacts, canManage, onChanged }: Props) {
  const [draft, setDraft] = useState<Draft | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  function edit(contact: ClientContact) {
    setDraft({
      id: contact.id,
      name: contact.name,
      email: contact.email,
      phone: contact.phone ?? "",
      designation: contact.designation ?? "",
      primaryContact: contact.primaryContact,
    });
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!draft) return;
    setBusy(true);
    setError(null);
    try {
      const body = {
        name: draft.name,
        email: draft.email,
        phone: draft.phone,
        designation: draft.designation,
        primaryContact: draft.primaryContact,
      };
      if (draft.id) {
        await api(`/clients/${clientId}/contacts/${draft.id}`, { method: "PUT", body });
      } else {
        await api(`/clients/${clientId}/contacts`, { method: "POST", body });
      }
      setDraft(null);
      onChanged();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function remove(contact: ClientContact) {
    if (!(await confirmDialog(t("contacts.confirmRemove")))) return;
    setError(null);
    try {
      await api(`/clients/${clientId}/contacts/${contact.id}`, { method: "DELETE" });
      onChanged();
    } catch (failure) {
      setError(failure);
    }
  }

  return (
    <div>
      <ErrorBanner error={error} />
      {canManage && !draft && (
        <Button type="button" onClick={() => setDraft({ ...blank })}>
          {t("contacts.add")}
        </Button>
      )}
      {draft && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void save(e)}>
            <h2>{draft.id ? t("contacts.edit") : t("contacts.add")}</h2>
            <div className="form-grid">
              <Field label={t("contacts.name")}>
                <Input required value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
              </Field>
              <Field label={t("contacts.email")}>
                <Input
                  type="email"
                  required
                  value={draft.email}
                  onChange={(e) => setDraft({ ...draft, email: e.target.value })}
                />
              </Field>
              <Field label={t("contacts.phone")}>
                <Input value={draft.phone} onChange={(e) => setDraft({ ...draft, phone: e.target.value })} />
              </Field>
              <Field label={t("contacts.designation")}>
                <Input
                  value={draft.designation}
                  onChange={(e) => setDraft({ ...draft, designation: e.target.value })}
                />
              </Field>
            </div>
            <label className="check">
              <CheckboxField
                checked={draft.primaryContact}
                onChange={(e) => setDraft({ ...draft, primaryContact: e.target.checked })}
              />
              {t("contacts.primary")}
            </label>
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("common.save")}
              </Button>
              <Button variant="outline" type="button" onClick={() => setDraft(null)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        </Card>
      )}
      {contacts.length === 0 ? (
        <p className="muted">{t("contacts.none")}</p>
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("contacts.name")}</TableHead>
                <TableHead>{t("contacts.email")}</TableHead>
                <TableHead>{t("contacts.phone")}</TableHead>
                <TableHead>{t("contacts.designation")}</TableHead>
                {canManage && <TableHead>{t("common.actions")}</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {contacts.map((contact) => (
                <TableRow key={contact.id}>
                  <TableCell>
                    {contact.name}
                    {contact.primaryContact && <Badge>{t("contacts.primaryBadge")}</Badge>}
                  </TableCell>
                  <TableCell>{contact.email}</TableCell>
                  <TableCell>{contact.phone}</TableCell>
                  <TableCell>{contact.designation}</TableCell>
                  {canManage && (
                    <TableCell className="row-actions">
                      <Button variant="outline" type="button" onClick={() => edit(contact)}>
                        {t("common.edit")}
                      </Button>
                      <Button variant="outline" type="button" onClick={() => void remove(contact)}>
                        {t("common.delete")}
                      </Button>
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
