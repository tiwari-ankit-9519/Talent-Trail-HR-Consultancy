"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import OfferForm from "@/components/OfferForm";
import PlacementCommission from "@/components/PlacementCommission";
import PlacementExit from "@/components/PlacementExit";
import PlacementInvoice from "@/components/PlacementInvoice";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api, apiBlob } from "@/lib/api";
import { formatDate, formatDay, formatMoney, t, tx } from "@/lib/messages";
import type { PlacementDetail } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { Input } from "@/components/shadcn/input";
import { Badge, badgeVariants } from "@/components/shadcn/badge";
import { Card } from "@/components/shadcn/card";
import { useBreadcrumbLabel } from "@/lib/breadcrumb";
import StatusPill from "@/components/StatusPill";
import { usePolling } from "@/lib/usePolling";
import { DateField } from "@/components/shadcn/date-picker";

type Action = "accept" | "decline" | "joined" | "notJoined" | null;

function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export default function PlacementDetailView({ id }: { id: string }) {
  const { can } = useAuth();
  const canManage = can("PLACEMENTS_MANAGE");
  const [placement, setPlacement] = useState<PlacementDetail | null>(null);
  useBreadcrumbLabel(placement?.candidateName);
  const [error, setError] = useState<unknown>(null);
  const [editing, setEditing] = useState(false);
  const [action, setAction] = useState<Action>(null);
  const [date, setDate] = useState(today());
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setPlacement(await api<PlacementDetail>(`/placements/${id}`));
    } catch (failure) {
      setError(failure);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  function open(next: Action) {
    setError(null);
    setReason("");
    setDate(today());
    setAction(next);
  }

  async function run(event: FormEvent) {
    event.preventDefault();
    if (!placement || !action) return;
    setBusy(true);
    setError(null);
    const version = placement.version;
    try {
      let updated: PlacementDetail;
      if (action === "accept") {
        updated = await api(`/placements/${id}/accept`, { method: "POST", body: { acceptedOn: date, version } });
      } else if (action === "joined") {
        updated = await api(`/placements/${id}/joined`, { method: "POST", body: { joiningDate: date, version } });
      } else if (action === "decline") {
        updated = await api(`/placements/${id}/decline`, { method: "POST", body: { reason, version } });
      } else {
        updated = await api(`/placements/${id}/not-joined`, { method: "POST", body: { reason, version } });
      }
      setPlacement(updated);
      setAction(null);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function upload(file: File | undefined) {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const data = new FormData();
      data.append("file", file);
      setPlacement(await api<PlacementDetail>(`/placements/${id}/joining-email`, { method: "POST", body: data }));
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
      if (fileInput.current) fileInput.current.value = "";
    }
  }

  async function sendOfferEmail() {
    setBusy(true);
    setError(null);
    try {
      setPlacement(await api<PlacementDetail>(`/placements/${id}/offer-email`, { method: "POST" }));
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function download() {
    setError(null);
    try {
      const { blob, fileName } = await apiBlob(`/placements/${id}/joining-email`);
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

  usePolling(placement?.offerEmailStatus === "QUEUED", load);

  if (!placement) {
    return (
      <section>
        <ErrorBanner error={error} />
        {!error && <Loading />}
      </section>
    );
  }

  const open_ = placement.status === "OFFERED" || placement.status === "OFFER_ACCEPTED";
  const needsReason = action === "decline" || action === "notJoined";

  return (
    <section>
      <div className="page-head">
        <h1>
          {placement.candidateName} <StatusPill kind="placementStatus" value={placement.status} />
        </h1>
        {canManage && open_ && !action && !editing && (
          <div className="actions">
            {placement.status === "OFFERED" && (
              <>
                <Button type="button" onClick={() => open("accept")}>
                  {t("placements.accept")}
                </Button>
                <Button variant="outline" type="button" onClick={() => open("decline")}>
                  {t("placements.decline")}
                </Button>
              </>
            )}
            {placement.status === "OFFER_ACCEPTED" && (
              <Button type="button" onClick={() => open("joined")}>
                {t("placements.recordJoined")}
              </Button>
            )}
            {placement.status === "OFFER_ACCEPTED" && (
              <Button variant="outline" type="button" onClick={() => open("notJoined")}>
                {t("placements.notJoined")}
              </Button>
            )}
            <Button variant="outline" type="button" onClick={() => setEditing(true)}>
              {t("placements.editOffer")}
            </Button>
          </div>
        )}
      </div>
      <ErrorBanner error={error} />

      {action && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void run(e)}>
            <h2>{t(`placements.action_${action}`)}</h2>
            {needsReason ? (
              <Field label={t("placements.reason")}>
                <Input required maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
              </Field>
            ) : (
              <Field label={action === "accept" ? t("placements.acceptedOn") : t("placements.joiningDate")}>
                <DateField
                  required
                  min={placement.offerDate}
                  max={today()}
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                />
              </Field>
            )}
            {action === "joined" && <p className="muted small">{t("placements.joinedHelp")}</p>}
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("common.save")}
              </Button>
              <Button variant="outline" type="button" disabled={busy} onClick={() => setAction(null)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        </Card>
      )}

      <div className="page-grid">
        {editing ? (
          <OfferForm
            submissionId={placement.submissionId}
            placement={placement}
            onCancel={() => setEditing(false)}
            onSaved={(updated) => {
              setPlacement(updated);
              setEditing(false);
            }}
          />
        ) : (
          <Card asChild>
            <div className="form-card">
              <dl className="dl-grid">
                <dt>{t("jobs.title")}</dt>
                <dd>
                  <Link href={`/jobs/${placement.jobId}`}>
                    {placement.jobCode} · {placement.jobTitle}
                  </Link>
                </dd>
                <dt>{t("jobs.client")}</dt>
                <dd>{placement.clientName}</dd>
                <dt>{t("candidates.fullName")}</dt>
                <dd>
                  <Link href={`/candidates/${placement.candidateId}`}>{placement.candidateName}</Link> ·{" "}
                  <Link href={`/submissions/${placement.submissionId}`}>{t("placements.viewSubmission")}</Link>
                </dd>
                <dt>{t("placements.offeredCtc")}</dt>
                <dd>{formatMoney(placement.offeredCtc)}</dd>
                <dt>{t("placements.offerDate")}</dt>
                <dd>{formatDay(placement.offerDate)}</dd>
                <dt>{t("placements.expectedJoiningDate")}</dt>
                <dd>{formatDay(placement.expectedJoiningDate)}</dd>
                {placement.acceptedOn && (
                  <>
                    <dt>{t("placements.acceptedOn")}</dt>
                    <dd>{formatDay(placement.acceptedOn)}</dd>
                  </>
                )}
                {placement.joiningDate && (
                  <>
                    <dt>{t("placements.joiningDate")}</dt>
                    <dd>{formatDay(placement.joiningDate)}</dd>
                  </>
                )}
                {placement.closeReason && (
                  <>
                    <dt>{t("placements.reason")}</dt>
                    <dd>{placement.closeReason}</dd>
                  </>
                )}
                {placement.notes && (
                  <>
                    <dt>{t("placements.notes")}</dt>
                    <dd>{placement.notes}</dd>
                  </>
                )}
              </dl>
            </div>
          </Card>
        )}

        {(placement.status === "OFFERED" || placement.status === "OFFER_ACCEPTED") && (
          <Card asChild>
            <div className="form-card">
              <h2>{t("placements.candidateEmail")}</h2>
              <p>
                <StatusPill kind="emailStatus" value={placement.offerEmailStatus} />
                {placement.offerEmailSentAt && <span className="muted"> {formatDate(placement.offerEmailSentAt)}</span>}
              </p>
              {placement.offerEmailError && <p className="muted small">{placement.offerEmailError}</p>}
              <p className="muted small">{t("placements.candidateEmailHelp")}</p>
              {canManage && (
                <div className="actions">
                  <Button
                    variant="outline"
                    type="button"
                    disabled={busy || placement.offerEmailStatus === "QUEUED"}
                    onClick={() => void sendOfferEmail()}
                  >
                    {placement.offerEmailStatus === "SENT" ? t("placements.sendAgain") : t("placements.sendEmail")}
                  </Button>
                </div>
              )}
            </div>
          </Card>
        )}

        {placement.status === "JOINED" && (
          <Card asChild>
            <div className="form-card">
              <h2>{t("placements.joiningEmail")}</h2>
              <p>
                <span
                  className={
                    placement.joiningEmailAttached
                      ? badgeVariants({ variant: "ok" })
                      : badgeVariants({ variant: "warn" })
                  }
                >
                  {placement.joiningEmailAttached ? t("placements.proofAttached") : t("placements.proofMissing")}
                </span>
              </p>
              {!placement.joiningEmailAttached && <p className="muted small">{t("placements.proofRule")}</p>}
              {placement.joiningEmailAttached && (
                <p>
                  {placement.joiningEmailFileName}
                  {placement.joiningEmailSize
                    ? ` (${Math.max(1, Math.round(placement.joiningEmailSize / 1024))} KB)`
                    : ""}{" "}
                  <Button variant="outline" type="button" onClick={() => void download()}>
                    {t("placements.downloadProof")}
                  </Button>
                </p>
              )}
              {canManage && (
                <div className="actions">
                  <Input
                    ref={fileInput}
                    type="file"
                    accept=".pdf,.eml,.msg,.png,.jpg,.jpeg"
                    disabled={busy}
                    onChange={(e) => void upload(e.target.files?.[0])}
                  />
                </div>
              )}
              <p className="muted small">{t("placements.proofHint")}</p>
            </div>
          </Card>
        )}

        {placement.status === "JOINED" && (
          <PlacementInvoice placementId={placement.id} proofAttached={placement.joiningEmailAttached} />
        )}

        {placement.status === "JOINED" && <PlacementCommission placementId={placement.id} />}

        <PlacementExit placementId={placement.id} status={placement.status} />
      </div>
    </section>
  );
}
