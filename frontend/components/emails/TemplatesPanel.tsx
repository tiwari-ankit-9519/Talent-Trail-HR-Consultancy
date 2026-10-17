"use client";

import { useEffect, useRef, useState } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { Input } from "@/components/shadcn/input";
import { Textarea } from "@/components/shadcn/textarea";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { confirmDialog } from "@/lib/confirm";
import { t, tx } from "@/lib/messages";
import type { EmailTemplate } from "@/lib/types";

type Preview = { subject: string; body: string };
type TestResult = { status: string; error: string | null; to: string };

export default function TemplatesPanel() {
  const [templates, setTemplates] = useState<EmailTemplate[] | null>(null);
  const [selected, setSelected] = useState<string>("");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [preview, setPreview] = useState<Preview | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const bodyRef = useRef<HTMLTextAreaElement | null>(null);

  function open(template: EmailTemplate) {
    setSelected(template.code);
    setSubject(template.subject);
    setBody(template.body);
    setPreview(null);
    setNotice(null);
    setError(null);
  }

  useEffect(() => {
    void (async () => {
      try {
        const list = await api<EmailTemplate[]>("/email-templates");
        setTemplates(list);
        if (list.length > 0) open(list[0]);
      } catch (failure) {
        setError(failure);
      }
    })();
  }, []);

  if (!templates) {
    return (
      <div className="mt-4">
        <ErrorBanner error={error} />
        {!error && <Loading />}
      </div>
    );
  }

  const current = templates.find((template) => template.code === selected);

  function replace(updated: EmailTemplate) {
    setTemplates((list) => (list ? list.map((item) => (item.code === updated.code ? updated : item)) : list));
    setSubject(updated.subject);
    setBody(updated.body);
  }

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await action();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  function insert(name: string) {
    const token = `{{${name}}}`;
    const area = bodyRef.current;
    if (!area) {
      setBody((value) => value + token);
      return;
    }
    const start = area.selectionStart ?? body.length;
    const end = area.selectionEnd ?? body.length;
    setBody(body.slice(0, start) + token + body.slice(end));
    requestAnimationFrame(() => {
      area.focus();
      area.setSelectionRange(start + token.length, start + token.length);
    });
  }

  const dirty = current ? subject !== current.subject || body !== current.body : false;

  return (
    <div className="mt-4 grid gap-4 lg:grid-cols-[18rem_1fr]">
      <div className="grid content-start gap-2">
        {templates.map((template) => (
          <Button
            key={template.code}
            type="button"
            variant={template.code === selected ? "default" : "outline"}
            className="h-auto justify-between gap-2 py-2 text-left"
            onClick={() => open(template)}
          >
            <span className="grid gap-0.5">
              <span>{template.name}</span>
              <span className="text-xs font-normal opacity-80">{tx(`emails.audience.${template.audience}`)}</span>
            </span>
            {template.customised && <Badge variant="accent">{t("emails.edited")}</Badge>}
          </Button>
        ))}
      </div>
      {current && (
        <Card className="grid gap-4 p-4">
          <div>
            <h2 className="m-0">{current.name}</h2>
            <p className="muted m-0">{current.description}</p>
          </div>
          <ErrorBanner error={error} />
          {notice && <Alert variant="success">{notice}</Alert>}
          <Field label={t("emails.subject")}>
            <Input value={subject} maxLength={200} onChange={(e) => setSubject(e.target.value)} />
          </Field>
          <Field label={t("emails.body")}>
            <Textarea ref={bodyRef} rows={14} value={body} maxLength={6000} onChange={(e) => setBody(e.target.value)} />
          </Field>
          <div className="grid gap-2">
            <span className="field-label">{t("emails.placeholders")}</span>
            <div className="flex flex-wrap gap-2">
              {current.variables.map((variable) => (
                <Button
                  key={variable.name}
                  type="button"
                  size="sm"
                  variant="outline"
                  title={variable.description}
                  onClick={() => insert(variable.name)}
                >
                  {`{{${variable.name}}}`}
                </Button>
              ))}
            </div>
            <span className="field-hint">{t("emails.placeholdersHint")}</span>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              disabled={busy || !dirty}
              onClick={() =>
                void run(async () => {
                  const updated = await api<EmailTemplate>(`/email-templates/${current.code}`, {
                    method: "PUT",
                    body: { subject, body },
                  });
                  replace(updated);
                  setNotice(t("emails.saved"));
                })
              }
            >
              {t("emails.save")}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={busy}
              onClick={() =>
                void run(async () => {
                  setPreview(
                    await api<Preview>(`/email-templates/${current.code}/preview`, {
                      method: "POST",
                      body: { subject, body },
                    }),
                  );
                })
              }
            >
              {t("emails.preview")}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={busy || dirty}
              onClick={() =>
                void run(async () => {
                  const result = await api<TestResult>(`/email-templates/${current.code}/test`, { method: "POST" });
                  setNotice(
                    result.status === "SENT"
                      ? `${t("emails.testSent")} ${result.to}`
                      : `${t("emails.testFailed")} ${result.error ?? result.status}`,
                  );
                })
              }
            >
              {t("emails.sendTest")}
            </Button>
            <Button
              type="button"
              variant="ghost"
              disabled={busy || !current.customised}
              onClick={() =>
                void run(async () => {
                  if (!(await confirmDialog(t("emails.confirmReset")))) return;
                  replace(await api<EmailTemplate>(`/email-templates/${current.code}/reset`, { method: "POST" }));
                  setPreview(null);
                  setNotice(t("emails.resetDone"));
                })
              }
            >
              {t("emails.reset")}
            </Button>
          </div>
          {dirty && <span className="field-hint">{t("emails.saveBeforeTest")}</span>}
          {preview && (
            <div className="grid gap-2 rounded-lg border border-line bg-bg p-4">
              <span className="field-label">{t("emails.previewTitle")}</span>
              <strong>{preview.subject}</strong>
              <pre className="m-0 whitespace-pre-wrap font-sans text-sm">{preview.body}</pre>
            </div>
          )}
        </Card>
      )}
    </div>
  );
}
