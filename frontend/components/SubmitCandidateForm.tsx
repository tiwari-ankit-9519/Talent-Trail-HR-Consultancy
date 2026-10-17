"use client";

import { useEffect, useState, type FormEvent } from "react";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { t, tx } from "@/lib/messages";
import type { CandidateSummary, ContactOption, SubmissionDetail } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Textarea } from "@/components/shadcn/textarea";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";

type Props = {
  jobId: string;
  candidate: CandidateSummary;
  onDone: (submission: SubmissionDetail) => void;
  onCancel: () => void;
};

export default function SubmitCandidateForm({ jobId, candidate, onDone, onCancel }: Props) {
  const [contacts, setContacts] = useState<ContactOption[] | null>(null);
  const [selected, setSelected] = useState<string[]>([]);
  const [note, setNote] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api<ContactOption[]>("/submissions/contacts", { query: { jobId } })
      .then((list) => {
        setContacts(list);
        const primary = list.filter((c) => c.primaryContact).map((c) => c.id);
        setSelected(primary.length > 0 ? primary : list.slice(0, 1).map((c) => c.id));
      })
      .catch(setError);
  }, [jobId]);

  function toggle(id: string) {
    setSelected((current) => (current.includes(id) ? current.filter((value) => value !== id) : [...current, id]));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await api<SubmissionDetail>("/submissions", {
        method: "POST",
        body: { jobId, candidateId: candidate.id, contactIds: selected, note },
      });
      onDone(result);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card asChild>
      <form className="form-card" onSubmit={(e) => void submit(e)}>
        <h2>
          {t("submissions.submitTitle")}: {candidate.fullName}
        </h2>
        <p className="muted small">{t("submissions.submitHelp")}</p>
        <ErrorBanner error={error} />
        {!contacts && !error && <Loading />}
        {contacts && contacts.length === 0 && <Alert variant="destructive">{t("submissions.noContacts")}</Alert>}
        {contacts && contacts.length > 0 && (
          <fieldset className="checks">
            <legend className="field-label">{t("submissions.recipients")}</legend>
            {contacts.map((contact) => (
              <label key={contact.id} className="check">
                <CheckboxField checked={selected.includes(contact.id)} onChange={() => toggle(contact.id)} />{" "}
                {contact.name} · {contact.email}
                {contact.designation ? ` · ${contact.designation}` : ""}
                {contact.primaryContact ? ` · ${tx("submissions.primary")}` : ""}
              </label>
            ))}
          </fieldset>
        )}
        <Field label={t("submissions.note")} hint={t("submissions.noteHint")}>
          <Textarea rows={3} maxLength={2000} value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
        <div className="actions">
          <Button type="submit" disabled={busy || selected.length === 0}>
            {busy ? t("submissions.sending") : t("submissions.submitAndSend")}
          </Button>
          <Button variant="outline" type="button" onClick={onCancel} disabled={busy}>
            {t("common.cancel")}
          </Button>
        </div>
      </form>
    </Card>
  );
}
