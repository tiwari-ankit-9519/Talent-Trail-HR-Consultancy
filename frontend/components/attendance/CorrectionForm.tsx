"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Textarea } from "@/components/shadcn/textarea";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { todayIso } from "@/lib/attendance";
import { t } from "@/lib/messages";
import type { AttendanceCorrection, WorkMode } from "@/lib/types";
import { DateField } from "@/components/shadcn/date-picker";
import { TimeField } from "@/components/shadcn/time-picker";

export default function CorrectionForm({ presetDate, onCreated }: { presetDate: string; onCreated: () => void }) {
  const [workDate, setWorkDate] = useState(presetDate || todayIso());
  const [checkIn, setCheckIn] = useState("10:00");
  const [checkOut, setCheckOut] = useState("19:00");
  const [workMode, setWorkMode] = useState<WorkMode>("OFFICE");
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (presetDate) setWorkDate(presetDate);
  }, [presetDate]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setDone(false);
    try {
      await api<AttendanceCorrection>("/attendance/requests", {
        method: "POST",
        body: { workDate, checkIn, checkOut: checkOut || null, workMode, reason },
      });
      setReason("");
      setDone(true);
      onCreated();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card className="p-5">
      <form onSubmit={submit} className="grid gap-4">
        <div>
          <h2 className="m-0 text-lg">{t("attendance.correction.heading")}</h2>
          <p className="muted small m-0 mt-1">{t("attendance.correction.help")}</p>
        </div>
        <ErrorBanner error={error} />
        {done && (
          <Alert variant="success">
            <p>{t("attendance.correction.sent")}</p>
          </Alert>
        )}
        <div className="form-grid">
          <Field label={t("attendance.correction.date")}>
            <DateField required max={todayIso()} value={workDate} onChange={(e) => setWorkDate(e.target.value)} />
          </Field>
          <Field label={t("attendance.correction.checkIn")}>
            <TimeField required value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />
          </Field>
          <Field label={t("attendance.correction.checkOut")}>
            <TimeField value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
          </Field>
          <Field label={t("attendance.today.workMode")}>
            <FormSelect value={workMode} onChange={(e) => setWorkMode(e.target.value as WorkMode)}>
              <option value="OFFICE">{t("enums.workMode.OFFICE")}</option>
              <option value="WFH">{t("enums.workMode.WFH")}</option>
            </FormSelect>
          </Field>
        </div>
        <Field label={t("attendance.correction.reason")}>
          <Textarea required maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>
        <div>
          <Button type="submit" disabled={busy}>
            {t("attendance.correction.submit")}
          </Button>
        </div>
      </form>
    </Card>
  );
}
