"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDay, t, tx } from "@/lib/messages";
import type { Page, PlacementReplacement, PlacementSummary, ReplacementView } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Badge, badgeVariants } from "@/components/shadcn/badge";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";
import StatusPill from "@/components/StatusPill";
import { DateField } from "@/components/shadcn/date-picker";

type Props = {
  placementId: string;
  status: string;
};

type Mode = "exit" | "link" | "close" | null;

function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export default function PlacementExit({ placementId, status }: Props) {
  const { can } = useAuth();
  const canView = can("REPLACEMENTS_VIEW") || can("REPLACEMENTS_MANAGE");
  const canManage = can("REPLACEMENTS_MANAGE");
  const [data, setData] = useState<PlacementReplacement | null>(null);
  const [mode, setMode] = useState<Mode>(null);
  const [exitDate, setExitDate] = useState(today());
  const [reason, setReason] = useState("");
  const [options, setOptions] = useState<PlacementSummary[]>([]);
  const [target, setTarget] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [localError, setLocalError] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!canView) return;
    try {
      setError(null);
      setData(await api<PlacementReplacement>(`/placements/${placementId}/replacement`));
    } catch (failure) {
      setError(failure);
    }
  }, [placementId, canView]);

  useEffect(() => {
    void load();
  }, [load]);

  async function openLink(original: ReplacementView) {
    setError(null);
    setTarget("");
    try {
      const result = await api<Page<PlacementSummary>>("/placements", {
        query: { jobId: original.jobId, clientId: original.clientId, size: 100 },
      });
      setOptions(
        result.items.filter(
          (p) => p.candidateId !== original.candidateId && p.status !== "OFFER_DECLINED" && p.status !== "NOT_JOINED",
        ),
      );
      setMode("link");
    } catch (failure) {
      setError(failure);
    }
  }

  async function submit(event: FormEvent, original: ReplacementView | null) {
    event.preventDefault();
    setLocalError("");
    setBusy(true);
    setError(null);
    try {
      if (mode === "exit") {
        if (exitDate > today()) {
          setLocalError(t("errors.EXIT_DATE_INVALID"));
          return;
        }
        await api<ReplacementView>(`/placements/${placementId}/exit`, { method: "POST", body: { exitDate, reason } });
      } else if (mode === "link" && original) {
        if (!target) {
          setLocalError(t("replacements.chooseReplacement"));
          return;
        }
        await api<ReplacementView>(`/replacements/${original.id}/link`, {
          method: "POST",
          body: { placementId: target, version: original.version },
        });
      } else if (mode === "close" && original) {
        await api<ReplacementView>(`/replacements/${original.id}/close`, {
          method: "POST",
          body: { note: reason, version: original.version },
        });
      }
      setMode(null);
      await load();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  if (!canView || !data) return canView ? <ErrorBanner error={error} /> : null;
  const original = data.asOriginal;
  const replacementOf = data.asReplacement;
  if (!original && !replacementOf && status !== "JOINED") return null;

  return (
    <Card asChild>
      <div className="form-card">
        <h2>{t("replacements.heading")}</h2>
        <ErrorBanner error={error} />
        {localError && (
          <Alert variant="destructive">
            <p>{localError}</p>
          </Alert>
        )}

        {replacementOf && (
          <p>
            <Badge variant="ok">{t("replacements.freeReplacement")}</Badge>{" "}
            <Link href={`/placements/${replacementOf.placementId}`}>{replacementOf.candidateName}</Link>{" "}
            {t("replacements.leftOn")} {formatDay(replacementOf.exitDate)}. {t("replacements.noInvoice")}
          </p>
        )}

        {original && (
          <>
            <dl className="dl-grid">
              <dt>{t("replacements.exitDate")}</dt>
              <dd>{formatDay(original.exitDate)}</dd>
              <dt>{t("replacements.reason")}</dt>
              <dd>{original.exitReason}</dd>
              <dt>{t("replacements.freeUntil")}</dt>
              <dd>
                {formatDay(original.freeUntil)} ({original.periodDays} {t("replacements.days")})
              </dd>
              <dt>{t("common.status")}</dt>
              <dd>
                <StatusPill kind="replacementStatus" value={original.status} />
              </dd>
              {original.replacementPlacementId && (
                <>
                  <dt>{t("replacements.replacedBy")}</dt>
                  <dd>
                    <Link href={`/placements/${original.replacementPlacementId}`}>
                      {original.replacementCandidateName}
                    </Link>
                  </dd>
                </>
              )}
              {original.closeNote && (
                <>
                  <dt>{t("replacements.closeNote")}</dt>
                  <dd>{original.closeNote}</dd>
                </>
              )}
            </dl>
            {original.status === "NOT_ELIGIBLE" && <p className="muted small">{t("replacements.notEligibleHelp")}</p>}
            {original.status === "OPEN" && <p className="muted small">{t("replacements.openHelp")}</p>}
            {canManage && original.status === "OPEN" && !mode && (
              <div className="actions">
                <Button type="button" onClick={() => void openLink(original)}>
                  {t("replacements.link")}
                </Button>
                <Button
                  variant="outline"
                  type="button"
                  onClick={() => {
                    setReason("");
                    setError(null);
                    setMode("close");
                  }}
                >
                  {t("replacements.close")}
                </Button>
              </div>
            )}
          </>
        )}

        {!original && status === "JOINED" && !mode && (
          <>
            <p className="muted small">{t("replacements.noExit")}</p>
            {canManage && (
              <div className="actions">
                <Button
                  variant="outline"
                  type="button"
                  onClick={() => {
                    setReason("");
                    setExitDate(today());
                    setError(null);
                    setMode("exit");
                  }}
                >
                  {t("replacements.recordExit")}
                </Button>
              </div>
            )}
          </>
        )}

        {mode && (
          <form onSubmit={(e) => void submit(e, original)}>
            {mode === "exit" && (
              <>
                <Field label={t("replacements.exitDate")}>
                  <DateField required max={today()} value={exitDate} onChange={(e) => setExitDate(e.target.value)} />
                </Field>
                <Field label={t("replacements.reason")}>
                  <Input required maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
                </Field>
              </>
            )}
            {mode === "link" && (
              <>
                <Field label={t("replacements.replacementCandidate")} hint={t("replacements.linkHelp")}>
                  <FormSelect required value={target} onChange={(e) => setTarget(e.target.value)}>
                    <option value="">{t("replacements.chooseReplacement")}</option>
                    {options.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.candidateName} · {tx(`enums.placementStatus.${p.status}`)}
                      </option>
                    ))}
                  </FormSelect>
                </Field>
                {options.length === 0 && <p className="muted small">{t("replacements.noOptions")}</p>}
              </>
            )}
            {mode === "close" && (
              <Field label={t("replacements.closeNote")} hint={t("replacements.closeHelp")}>
                <Input required maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
              </Field>
            )}
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("common.save")}
              </Button>
              <Button variant="outline" type="button" disabled={busy} onClick={() => setMode(null)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        )}
      </div>
    </Card>
  );
}
