"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Button } from "@/components/shadcn/button";
import { DateField } from "@/components/shadcn/date-picker";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/shadcn/dialog";
import { FormSelect } from "@/components/shadcn/select";
import { Textarea } from "@/components/shadcn/textarea";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { todayIso } from "@/lib/attendance";
import { t } from "@/lib/messages";
import type { HalfDay, LeaveApplication, LeaveBalance, LeavePreview } from "@/lib/types";

export default function LeaveApplyDialog({
  open,
  onOpenChange,
  balances,
  onApplied,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  balances: LeaveBalance[];
  onApplied: () => void;
}) {
  const [typeId, setTypeId] = useState("");
  const [fromDate, setFromDate] = useState(todayIso());
  const [toDate, setToDate] = useState(todayIso());
  const [duration, setDuration] = useState("FULL");
  const [reason, setReason] = useState("");
  const [preview, setPreview] = useState<LeavePreview | null>(null);
  const [previewError, setPreviewError] = useState<unknown>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (open) {
      setError(null);
      setReason("");
      setDuration("FULL");
      setTypeId((current) => current || balances[0]?.leaveTypeId || "");
    }
  }, [open, balances]);

  const single = fromDate === toDate;
  const halfDay: HalfDay | undefined = single && duration !== "FULL" ? (duration as HalfDay) : undefined;

  useEffect(() => {
    if (!single && duration !== "FULL") setDuration("FULL");
  }, [single, duration]);

  useEffect(() => {
    if (!open || !typeId || !fromDate || !toDate) {
      setPreview(null);
      return;
    }
    let cancelled = false;
    api<LeavePreview>("/attendance/leave/applications/preview", {
      query: { leaveTypeId: typeId, fromDate, toDate, halfDay },
    })
      .then((result) => {
        if (!cancelled) {
          setPreview(result);
          setPreviewError(null);
        }
      })
      .catch((failure) => {
        if (!cancelled) {
          setPreview(null);
          setPreviewError(failure);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [open, typeId, fromDate, toDate, halfDay]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api<LeaveApplication>("/attendance/leave/applications", {
        method: "POST",
        body: { leaveTypeId: typeId, fromDate, toDate, halfDay: halfDay ?? null, reason },
      });
      onOpenChange(false);
      onApplied();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent closeLabel={t("common.close")}>
        <DialogHeader>
          <DialogTitle>{t("attendance.apply.title")}</DialogTitle>
          <DialogDescription>{t("attendance.apply.help")}</DialogDescription>
        </DialogHeader>
        <form className="grid gap-3" onSubmit={(event) => void submit(event)}>
          <Field label={t("attendance.apply.type")}>
            <FormSelect value={typeId} required onChange={(e) => setTypeId(e.target.value)}>
              {balances.map((balance) => (
                <option key={balance.leaveTypeId} value={balance.leaveTypeId}>
                  {balance.unpaid
                    ? `${balance.name} (${t("attendance.apply.unpaidTag")})`
                    : `${balance.name} (${t("attendance.apply.left").replace("{n}", String(balance.remaining))})`}
                </option>
              ))}
            </FormSelect>
          </Field>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label={t("attendance.apply.from")}>
              <DateField
                required
                value={fromDate}
                onChange={(e) => {
                  setFromDate(e.target.value);
                  if (toDate < e.target.value) setToDate(e.target.value);
                }}
              />
            </Field>
            <Field label={t("attendance.apply.to")}>
              <DateField required min={fromDate} value={toDate} onChange={(e) => setToDate(e.target.value)} />
            </Field>
          </div>
          <Field label={t("attendance.apply.duration")}>
            <FormSelect value={duration} disabled={!single} onChange={(e) => setDuration(e.target.value)}>
              <option value="FULL">{t("attendance.apply.full")}</option>
              <option value="FIRST">{t("attendance.apply.first")}</option>
              <option value="SECOND">{t("attendance.apply.second")}</option>
            </FormSelect>
          </Field>
          <Field label={t("attendance.apply.reason")}>
            <Textarea required maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
          </Field>
          {preview && (
            <Alert variant={preview.enough ? "info" : "destructive"}>
              {t("attendance.apply.preview").replace("{n}", String(preview.days))}
              {!preview.unpaid && preview.remaining !== null && (
                <> {t("attendance.apply.previewLeft").replace("{n}", String(preview.remaining))}</>
              )}
              {preview.unpaid && <> {t("attendance.apply.previewUnpaid")}</>}
              {!preview.enough && <> {t("attendance.apply.previewShort")}</>}
            </Alert>
          )}
          <ErrorBanner error={previewError} />
          <ErrorBanner error={error} />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              {t("common.cancel")}
            </Button>
            <Button type="submit" disabled={busy || !typeId || !reason.trim() || (preview !== null && !preview.enough)}>
              {t("attendance.apply.submit")}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
