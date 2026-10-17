"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { Check, Download, Trash2, X } from "lucide-react";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { DateField } from "@/components/shadcn/date-picker";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/shadcn/dialog";
import { Input } from "@/components/shadcn/input";
import { Textarea } from "@/components/shadcn/textarea";
import { FormSelect } from "@/components/shadcn/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api, apiBlob } from "@/lib/api";
import { confirmDialog } from "@/lib/confirm";
import { t } from "@/lib/messages";
import type { ProfileDocument, ProfileDocumentType } from "@/lib/types";

const docTypes: ProfileDocumentType[] = [
  "ID_PROOF",
  "ADDRESS_PROOF",
  "OFFER_LETTER",
  "EDUCATION",
  "EXPERIENCE",
  "BANK_PROOF",
  "OTHER",
];

type Props = {
  userId: string;
  self: boolean;
  canManage: boolean;
  onCountChanged?: () => void;
};

function formatDate(value: string): string {
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function formatSize(bytes: number): string {
  return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

export default function DocumentsPanel({ userId, self, canManage, onCountChanged }: Props) {
  const [rows, setRows] = useState<ProfileDocument[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [docType, setDocType] = useState<ProfileDocumentType>("ID_PROOF");
  const [title, setTitle] = useState("");
  const [expiresOn, setExpiresOn] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [rejecting, setRejecting] = useState<ProfileDocument | null>(null);
  const [reason, setReason] = useState("");
  const input = useRef<HTMLInputElement>(null);
  const canUpload = canManage || self;
  const listPath = self ? "/profiles/me/documents" : `/profiles/${userId}/documents`;

  function load() {
    api<ProfileDocument[]>(listPath).then(setRows).catch(setError);
  }

  useEffect(load, [listPath]);

  async function upload(event: FormEvent) {
    event.preventDefault();
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const data = new FormData();
      data.append("docType", docType);
      data.append("title", title.trim());
      if (expiresOn) data.append("expiresOn", expiresOn);
      data.append("file", file);
      await api<ProfileDocument>(self ? "/profiles/me/documents" : `/profiles/${userId}/documents`, {
        method: "POST",
        body: data,
      });
      setTitle("");
      setExpiresOn("");
      setFile(null);
      if (input.current) input.current.value = "";
      load();
      onCountChanged?.();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function download(doc: ProfileDocument) {
    setError(null);
    try {
      const { blob, fileName } = await apiBlob(`/profiles/documents/${doc.id}/file`);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (failure) {
      setError(failure);
    }
  }

  async function remove(doc: ProfileDocument) {
    if (!(await confirmDialog(t("profile.documents.deleteConfirm").replace("{title}", doc.title)))) return;
    setError(null);
    try {
      await api<void>(`/profiles/documents/${doc.id}`, { method: "DELETE" });
      load();
      onCountChanged?.();
    } catch (failure) {
      setError(failure);
    }
  }

  async function review(doc: ProfileDocument, decision: "VERIFIED" | "REJECTED", note: string) {
    setError(null);
    try {
      await api<ProfileDocument>(`/profiles/documents/${doc.id}/review`, {
        method: "POST",
        body: { decision, note: note.trim() || undefined },
      });
      setRejecting(null);
      setReason("");
      load();
    } catch (failure) {
      setError(failure);
    }
  }

  return (
    <div className="grid gap-4">
      <ErrorBanner error={error} />
      {self && !canManage && <p className="muted m-0 text-sm">{t("profile.documents.reviewNotice")}</p>}
      {canUpload && (
        <Card className="p-5">
          <form className="grid gap-3" onSubmit={(event) => void upload(event)}>
            <h2 className="m-0 text-base font-semibold">{t("profile.documents.add")}</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label={t("profile.documents.type")}>
                <FormSelect value={docType} onChange={(e) => setDocType(e.target.value as ProfileDocumentType)}>
                  {docTypes.map((type) => (
                    <option key={type} value={type}>
                      {t(`profile.documents.types.${type}`)}
                    </option>
                  ))}
                </FormSelect>
              </Field>
              <Field label={t("profile.documents.title")}>
                <Input required maxLength={150} value={title} onChange={(e) => setTitle(e.target.value)} />
              </Field>
              <Field label={t("profile.documents.expiresOn")} hint={t("profile.documents.expiresHint")}>
                <DateField value={expiresOn} onChange={(e) => setExpiresOn(e.target.value)} />
              </Field>
              <Field label={t("profile.documents.file")} hint={t("profile.documents.fileHint")}>
                <Input
                  ref={input}
                  required
                  type="file"
                  accept=".pdf,.png,.jpg,.jpeg,.docx"
                  onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                />
              </Field>
            </div>
            <div>
              <Button type="submit" disabled={busy || !file || title.trim() === ""}>
                {busy ? t("profile.documents.uploading") : t("profile.documents.upload")}
              </Button>
            </div>
          </form>
        </Card>
      )}
      {!rows && !error && <Loading />}
      {rows && rows.length === 0 && <p className="muted m-0">{t("profile.documents.none")}</p>}
      {rows && rows.length > 0 && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("profile.documents.title")}</TableHead>
                <TableHead>{t("profile.documents.type")}</TableHead>
                <TableHead>{t("profile.documents.expiresOn")}</TableHead>
                <TableHead>{t("profile.documents.status")}</TableHead>
                <TableHead>{t("common.actions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((doc) => (
                <TableRow key={doc.id}>
                  <TableCell>
                    <div className="font-medium">{doc.title}</div>
                    <div className="muted small">
                      {doc.fileName} · {formatSize(doc.fileSize)}
                    </div>
                  </TableCell>
                  <TableCell>{t(`profile.documents.types.${doc.docType}`)}</TableCell>
                  <TableCell className="whitespace-nowrap">
                    {doc.expiresOn ? formatDate(doc.expiresOn) : "—"}{" "}
                    {doc.expired && <Badge variant="bad">{t("profile.documents.expired")}</Badge>}
                    {doc.expiringSoon && <Badge variant="warn">{t("profile.documents.expiringSoon")}</Badge>}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={doc.reviewStatus === "VERIFIED" ? "ok" : doc.reviewStatus === "PENDING" ? "warn" : "bad"}
                    >
                      {t(`profile.documents.review.${doc.reviewStatus}`)}
                    </Badge>
                    {doc.uploadedBySelf && <div className="muted small">{t("profile.documents.byEmployee")}</div>}
                    {doc.reviewStatus === "REJECTED" && doc.reviewNote && (
                      <div className="muted small">{doc.reviewNote}</div>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-2">
                      {doc.canReview && (
                        <>
                          <Button type="button" size="sm" onClick={() => void review(doc, "VERIFIED", "")}>
                            <Check aria-hidden="true" className="size-4" />
                            {t("profile.documents.verify")}
                          </Button>
                          <Button type="button" size="sm" variant="outline" onClick={() => setRejecting(doc)}>
                            <X aria-hidden="true" className="size-4" />
                            {t("profile.documents.reject")}
                          </Button>
                        </>
                      )}
                      <Button type="button" size="sm" variant="outline" onClick={() => void download(doc)}>
                        <Download aria-hidden="true" className="size-4" />
                        {t("profile.documents.download")}
                      </Button>
                      {doc.canDelete && (
                        <Button type="button" size="sm" variant="outline" onClick={() => void remove(doc)}>
                          <Trash2 aria-hidden="true" className="size-4" />
                          {t("common.delete")}
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      <Dialog open={rejecting !== null} onOpenChange={(open) => !open && setRejecting(null)}>
        <DialogContent closeLabel={t("common.close")}>
          <DialogHeader>
            <DialogTitle>{t("profile.documents.rejectTitle").replace("{title}", rejecting?.title ?? "")}</DialogTitle>
            <DialogDescription>{t("profile.documents.rejectHelp")}</DialogDescription>
          </DialogHeader>
          <Field label={t("profile.documents.rejectReason")}>
            <Textarea rows={3} maxLength={300} value={reason} onChange={(e) => setReason(e.target.value)} />
          </Field>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setRejecting(null)}>
              {t("common.cancel")}
            </Button>
            <Button
              type="button"
              disabled={reason.trim() === ""}
              onClick={() => rejecting && void review(rejecting, "REJECTED", reason)}
            >
              {t("profile.documents.reject")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
