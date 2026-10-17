"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { t, tx } from "@/lib/messages";
import type { ReminderRunResult, ReminderSetting } from "@/lib/types";

type Draft = { enabled: boolean; offsets: string; extras: string };

function toDraft(setting: ReminderSetting): Draft {
  return { enabled: setting.enabled, offsets: setting.offsets.join(", "), extras: setting.extraRecipients.join(", ") };
}

export default function RemindersPanel() {
  const [items, setItems] = useState<ReminderSetting[] | null>(null);
  const [drafts, setDrafts] = useState<Record<string, Draft>>({});
  const [error, setError] = useState<unknown>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [saving, setSaving] = useState<Record<string, boolean>>({});
  const [running, setRunning] = useState(false);
  const [cardError, setCardError] = useState<Record<string, unknown>>({});
  const [cardNotice, setCardNotice] = useState<Record<string, string>>({});

  useEffect(() => {
    void (async () => {
      try {
        const list = await api<ReminderSetting[]>("/reminders");
        setItems(list);
        setDrafts(Object.fromEntries(list.map((item) => [item.code, toDraft(item)])));
      } catch (failure) {
        setError(failure);
      }
    })();
  }, []);

  if (!items) {
    return (
      <div className="mt-4">
        <ErrorBanner error={error} />
        {!error && <Loading />}
      </div>
    );
  }

  function patch(code: string, change: Partial<Draft>) {
    setDrafts((current) => ({ ...current, [code]: { ...current[code], ...change } }));
  }

  function mark<T>(set: (update: (current: Record<string, T>) => Record<string, T>) => void, code: string, value?: T) {
    set((current) => {
      const next = { ...current };
      if (value === undefined) delete next[code];
      else next[code] = value;
      return next;
    });
  }

  function isDirty(item: ReminderSetting): boolean {
    const draft = drafts[item.code];
    return !!draft && JSON.stringify(draft) !== JSON.stringify(toDraft(item));
  }

  async function save(item: ReminderSetting) {
    const draft = drafts[item.code];
    const parts = draft.offsets
      .split(/[,\s]+/)
      .map((part) => part.trim())
      .filter(Boolean);
    const offsets = parts.map(Number);
    mark(setCardNotice, item.code);
    if (offsets.length === 0 || offsets.some((value) => !Number.isInteger(value))) {
      mark(setCardError, item.code, t("emails.offsetsInvalid"));
      return;
    }
    mark(setSaving, item.code, true);
    mark(setCardError, item.code);
    try {
      const updated = await api<ReminderSetting>(`/reminders/${item.code}`, {
        method: "PUT",
        body: {
          enabled: draft.enabled,
          offsets,
          extraRecipients: draft.extras
            .split(/[,;\s]+/)
            .map((value) => value.trim())
            .filter(Boolean),
        },
      });
      setItems((list) => (list ? list.map((entry) => (entry.code === updated.code ? updated : entry)) : list));
      patch(updated.code, toDraft(updated));
      mark(setCardNotice, item.code, t("emails.reminderSaved"));
    } catch (failure) {
      mark(setCardError, item.code, failure);
    } finally {
      mark(setSaving, item.code);
    }
  }

  async function runNow() {
    setRunning(true);
    setError(null);
    setNotice(null);
    try {
      const result = await api<ReminderRunResult>("/reminders/run", { method: "POST" });
      if (!result.mailConfigured) {
        setNotice(t("emails.runNoMail"));
      } else {
        const total = Object.values(result.sent).reduce((sum, value) => sum + value, 0);
        setNotice(`${t("emails.runDone")} ${total}`);
      }
    } catch (failure) {
      setError(failure);
    } finally {
      setRunning(false);
    }
  }

  return (
    <div className="mt-4 grid gap-4">
      <Alert variant="info">{t("emails.remindersIntro")}</Alert>
      <ErrorBanner error={error} />
      {notice && <Alert variant="success">{notice}</Alert>}
      <div>
        <Button type="button" variant="outline" disabled={running} onClick={() => void runNow()}>
          {t("emails.runNow")}
        </Button>
      </div>
      {items.map((item) => {
        const draft = drafts[item.code] ?? toDraft(item);
        return (
          <Card key={item.code} className="grid gap-4 p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="m-0">{item.name}</h2>
                <p className="muted m-0">{item.description}</p>
              </div>
              <Badge variant={item.enabled ? "ok" : "default"}>{item.enabled ? t("emails.on") : t("emails.off")}</Badge>
            </div>
            <label className="flex items-center gap-2 text-sm">
              <CheckboxField
                checked={draft.enabled}
                onChange={(e) => {
                  mark(setCardNotice, item.code);
                  patch(item.code, { enabled: e.target.checked });
                }}
              />
              {t("emails.enable")}
            </label>
            <div className="form-grid">
              <Field label={t("emails.offsets")} hint={item.offsetsHelp}>
                <Input value={draft.offsets} onChange={(e) => patch(item.code, { offsets: e.target.value })} />
              </Field>
              <Field label={t("emails.extraRecipients")} hint={t("emails.extraRecipientsHint")}>
                <Input value={draft.extras} onChange={(e) => patch(item.code, { extras: e.target.value })} />
              </Field>
            </div>
            <span className="field-hint">
              {t("emails.sentTo")}: {tx(`emails.audience.${item.audience}`)} · {t("emails.counted")}: {item.reference}
            </span>
            <div>
              <Button
                type="button"
                disabled={saving[item.code] === true || !isDirty(item)}
                onClick={() => void save(item)}
              >
                {t("emails.save")}
              </Button>
            </div>
            {typeof cardError[item.code] === "string" && (
              <Alert variant="destructive">{String(cardError[item.code])}</Alert>
            )}
            {cardError[item.code] !== undefined && typeof cardError[item.code] !== "string" && (
              <ErrorBanner error={cardError[item.code]} />
            )}
            {cardNotice[item.code] && !isDirty(item) && <Alert variant="success">{cardNotice[item.code]}</Alert>}
          </Card>
        );
      })}
    </div>
  );
}
