"use client";

import { useRef, useState } from "react";
import { ErrorBanner } from "@/components/ui";
import { api, apiBlob } from "@/lib/api";
import { t } from "@/lib/messages";
import type { CandidateDetail } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { Input } from "@/components/shadcn/input";
import { Card } from "@/components/shadcn/card";

type Props = {
  candidate: CandidateDetail;
  canManage: boolean;
  onChanged: (candidate: CandidateDetail) => void;
};

export default function CandidateResume({ candidate, canManage, onChanged }: Props) {
  const input = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  async function download() {
    setError(null);
    try {
      const { blob, fileName } = await apiBlob(`/candidates/${candidate.id}/resume`);
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

  async function upload(file: File | undefined) {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const data = new FormData();
      data.append("file", file);
      onChanged(await api<CandidateDetail>(`/candidates/${candidate.id}/resume`, { method: "POST", body: data }));
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
      if (input.current) input.current.value = "";
    }
  }

  return (
    <Card asChild>
      <div className="form-card">
        <h2>{t("candidates.resume")}</h2>
        <ErrorBanner error={error} />
        {candidate.hasResume ? (
          <p>
            {candidate.resumeFileName}
            {candidate.resumeSize ? ` (${Math.max(1, Math.round(candidate.resumeSize / 1024))} KB)` : ""}{" "}
            <Button variant="outline" type="button" onClick={() => void download()}>
              {t("candidates.downloadResume")}
            </Button>
          </p>
        ) : (
          <p className="muted">{t("candidates.noResume")}</p>
        )}
        {canManage && (
          <div className="actions">
            <Input
              ref={input}
              type="file"
              accept=".pdf,.doc,.docx"
              disabled={busy}
              onChange={(e) => void upload(e.target.files?.[0])}
            />
          </div>
        )}
        <p className="muted small">{t("candidates.resumeHint")}</p>
      </div>
    </Card>
  );
}
