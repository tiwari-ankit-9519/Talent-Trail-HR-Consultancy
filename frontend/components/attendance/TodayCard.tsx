"use client";

import { Clock, LogIn, LogOut } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { ErrorBanner, Field } from "@/components/ui";
import StatusPill from "@/components/StatusPill";
import { api } from "@/lib/api";
import { formatMinutes, formatTime } from "@/lib/attendance";
import { formatDay, t, tx } from "@/lib/messages";
import type { AttendanceToday, WorkMode } from "@/lib/types";

export default function TodayCard({
  today,
  onChange,
}: {
  today: AttendanceToday;
  onChange: (next: AttendanceToday) => void;
}) {
  const [mode, setMode] = useState<WorkMode>("OFFICE");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function run(path: string, body?: unknown) {
    setBusy(true);
    setError(null);
    try {
      onChange(await api<AttendanceToday>(path, { method: "POST", body }));
      setNote("");
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card className="grid gap-4 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="m-0 flex items-center gap-2 text-lg">
            <Clock aria-hidden="true" className="size-5 text-accent" />
            {t("attendance.today.heading")}
          </h2>
          <p className="muted small m-0 mt-1">{formatDay(today.date)}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {today.dayType !== "WORKING" && (
            <Badge variant="accent">{today.holidayName ?? tx(`enums.attendanceState.${today.dayType}`)}</Badge>
          )}
          {today.optionalHolidayName && (
            <Badge variant="warn">
              {t("attendance.today.optionalDay").replace("{name}", today.optionalHolidayName)}
            </Badge>
          )}
          {today.leaveKind && <Badge variant="accent">{tx(`enums.leaveKind.${today.leaveKind}`)}</Badge>}
          {today.status && <StatusPill kind="attendanceState" value={today.status} />}
          {today.late && <Badge variant="warn">{t("attendance.late")}</Badge>}
          {today.workMode === "WFH" && <Badge variant="accent">{t("attendance.wfh")}</Badge>}
        </div>
      </div>

      <ErrorBanner error={error} />

      {!today.checkedIn && (
        <div className="grid gap-3">
          <div className="form-grid">
            <Field label={t("attendance.today.workMode")}>
              <FormSelect value={mode} onChange={(e) => setMode(e.target.value as WorkMode)}>
                <option value="OFFICE">{t("enums.workMode.OFFICE")}</option>
                <option value="WFH">{t("enums.workMode.WFH")}</option>
              </FormSelect>
            </Field>
            <Field label={t("attendance.today.note")}>
              <Input value={note} maxLength={300} onChange={(e) => setNote(e.target.value)} />
            </Field>
          </div>
          <p className="muted small m-0">
            {t("attendance.today.shiftHint").replace("{time}", today.shiftStart)}
            {today.dayType !== "WORKING" ? ` ${t("attendance.today.offDayHint")}` : ""}
          </p>
          <div>
            <Button
              type="button"
              disabled={busy}
              onClick={() => void run("/attendance/check-in", { workMode: mode, note })}
            >
              <LogIn aria-hidden="true" className="size-4" />
              {t("attendance.today.checkIn")}
            </Button>
          </div>
        </div>
      )}

      {today.checkedIn && (
        <div className="grid gap-3">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <div>
              <p className="muted small m-0">{t("attendance.today.in")}</p>
              <p className="m-0 text-lg font-semibold">{formatTime(today.checkIn)}</p>
            </div>
            <div>
              <p className="muted small m-0">{t("attendance.today.out")}</p>
              <p className="m-0 text-lg font-semibold">{today.checkedOut ? formatTime(today.checkOut) : "—"}</p>
            </div>
            <div>
              <p className="muted small m-0">{t("attendance.today.worked")}</p>
              <p className="m-0 text-lg font-semibold">
                {today.workedMinutes !== null ? formatMinutes(today.workedMinutes) : "—"}
              </p>
            </div>
          </div>
          {!today.checkedOut ? (
            <div>
              <Button type="button" variant="outline" disabled={busy} onClick={() => void run("/attendance/check-out")}>
                <LogOut aria-hidden="true" className="size-4" />
                {t("attendance.today.checkOut")}
              </Button>
            </div>
          ) : (
            <p className="muted small m-0">{t("attendance.today.done")}</p>
          )}
        </div>
      )}
    </Card>
  );
}
