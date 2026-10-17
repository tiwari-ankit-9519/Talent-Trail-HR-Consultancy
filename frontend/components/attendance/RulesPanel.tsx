"use client";

import { Trash2 } from "lucide-react";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Empty, ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { weekdays } from "@/lib/attendance";
import { confirmDialog } from "@/lib/confirm";
import { formatDay, t, tx } from "@/lib/messages";
import StatusPill from "@/components/StatusPill";
import { FormSelect } from "@/components/shadcn/select";
import type { AttendanceHoliday, AttendanceSettings, HolidayKind } from "@/lib/types";
import { DateField } from "@/components/shadcn/date-picker";
import { TimeField } from "@/components/shadcn/time-picker";

export default function RulesPanel() {
  const [settings, setSettings] = useState<AttendanceSettings | null>(null);
  const [saved, setSaved] = useState(false);
  const [year, setYear] = useState(new Date().getFullYear());
  const [holidays, setHolidays] = useState<AttendanceHoliday[]>([]);
  const [holidayDate, setHolidayDate] = useState("");
  const [holidayName, setHolidayName] = useState("");
  const [holidayKind, setHolidayKind] = useState<HolidayKind>("NATIONAL");
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const loadSettings = useCallback(async () => {
    try {
      setSettings(await api<AttendanceSettings>("/attendance/settings"));
    } catch (failure) {
      setError(failure);
    }
  }, []);

  const loadHolidays = useCallback(async () => {
    try {
      setHolidays(await api<AttendanceHoliday[]>("/attendance/holidays", { query: { year } }));
    } catch (failure) {
      setError(failure);
    }
  }, [year]);

  useEffect(() => {
    void loadSettings();
  }, [loadSettings]);

  useEffect(() => {
    void loadHolidays();
  }, [loadHolidays]);

  function patch(next: Partial<AttendanceSettings>) {
    setSaved(false);
    setSettings((current) => (current ? { ...current, ...next } : current));
  }

  function toggleDay(day: string, checked: boolean) {
    if (!settings) return;
    const next = checked ? [...settings.weeklyOff, day] : settings.weeklyOff.filter((entry) => entry !== day);
    patch({ weeklyOff: next });
  }

  async function saveSettings(event: FormEvent) {
    event.preventDefault();
    if (!settings) return;
    setBusy(true);
    setError(null);
    try {
      setSettings(await api<AttendanceSettings>("/attendance/settings", { method: "PUT", body: settings }));
      setSaved(true);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function addHoliday(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api<AttendanceHoliday>("/attendance/holidays", {
        method: "POST",
        body: { date: holidayDate, name: holidayName, kind: holidayKind },
      });
      setHolidayDate("");
      setHolidayName("");
      await loadHolidays();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function removeHoliday(holiday: AttendanceHoliday) {
    if (!(await confirmDialog(t("attendance.rules.removeConfirm").replace("{name}", holiday.name)))) return;
    setError(null);
    try {
      await api<void>(`/attendance/holidays/${holiday.id}`, { method: "DELETE" });
      await loadHolidays();
    } catch (failure) {
      setError(failure);
    }
  }

  return (
    <div className="grid gap-4">
      <ErrorBanner error={error} />
      <div className="page-grid">
        {!settings && !error ? (
          <Loading />
        ) : (
          settings && (
            <Card className="p-5">
              <form onSubmit={saveSettings} className="grid gap-4">
                <div>
                  <h2 className="m-0 text-lg">{t("attendance.rules.heading")}</h2>
                  <p className="muted small m-0 mt-1">{t("attendance.rules.help")}</p>
                </div>
                {saved && (
                  <Alert variant="success">
                    <p>{t("common.saved")}</p>
                  </Alert>
                )}
                <div className="form-grid">
                  <Field label={t("attendance.rules.shiftStart")}>
                    <TimeField
                      required
                      value={settings.shiftStart}
                      onChange={(e) => patch({ shiftStart: e.target.value })}
                    />
                  </Field>
                  <Field label={t("attendance.rules.grace")} hint={t("attendance.rules.graceHint")}>
                    <Input
                      type="number"
                      min={0}
                      max={240}
                      value={settings.lateGraceMinutes}
                      onChange={(e) => patch({ lateGraceMinutes: Number(e.target.value) })}
                    />
                  </Field>
                  <Field label={t("attendance.rules.halfDay")} hint={t("attendance.rules.halfDayHint")}>
                    <Input
                      type="number"
                      min={30}
                      max={600}
                      value={settings.halfDayMinutes}
                      onChange={(e) => patch({ halfDayMinutes: Number(e.target.value) })}
                    />
                  </Field>
                  <Field label={t("attendance.rules.fullDay")} hint={t("attendance.rules.fullDayHint")}>
                    <Input
                      type="number"
                      min={60}
                      max={1000}
                      value={settings.fullDayMinutes}
                      onChange={(e) => patch({ fullDayMinutes: Number(e.target.value) })}
                    />
                  </Field>
                  <Field label={t("attendance.rules.optionalQuota")} hint={t("attendance.rules.optionalQuotaHint")}>
                    <Input
                      type="number"
                      min={0}
                      max={30}
                      value={settings.optionalQuota}
                      onChange={(e) => patch({ optionalQuota: Number(e.target.value) })}
                    />
                  </Field>
                  <Field label={t("attendance.rules.compOffValid")} hint={t("attendance.rules.compOffValidHint")}>
                    <Input
                      type="number"
                      min={1}
                      max={365}
                      value={settings.compOffValidDays}
                      onChange={(e) => patch({ compOffValidDays: Number(e.target.value) })}
                    />
                  </Field>
                </div>
                <fieldset className="m-0 grid gap-2 border-0 p-0">
                  <legend className="field-label mb-1 p-0">{t("attendance.rules.weeklyOff")}</legend>
                  <div className="flex flex-wrap gap-2">
                    {weekdays.map((day) => (
                      <label
                        key={day}
                        className="flex cursor-pointer items-center gap-2 rounded-lg border border-line bg-bg px-3 py-1.5 text-sm text-fg transition-colors hover:border-accent has-[[data-state=checked]]:border-accent-solid has-[[data-state=checked]]:bg-[color-mix(in_oklab,var(--accent-solid)_16%,var(--surface))]"
                      >
                        <CheckboxField
                          checked={settings.weeklyOff.includes(day)}
                          onChange={(e) => toggleDay(day, e.target.checked)}
                        />
                        {tx(`attendance.weekdays.${day}`)}
                      </label>
                    ))}
                  </div>
                </fieldset>
                <div>
                  <Button type="submit" disabled={busy}>
                    {t("common.save")}
                  </Button>
                </div>
              </form>
            </Card>
          )
        )}

        <Card className="grid content-start gap-4 p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="m-0 text-lg">{t("attendance.holidays.heading")}</h2>
            <Input
              type="number"
              aria-label={t("attendance.holidays.year")}
              className="w-28"
              value={year}
              onChange={(e) => setYear(Number(e.target.value) || new Date().getFullYear())}
            />
          </div>
          <form onSubmit={addHoliday} className="form-grid">
            <Field label={t("attendance.holidays.date")}>
              <DateField required value={holidayDate} onChange={(e) => setHolidayDate(e.target.value)} />
            </Field>
            <Field label={t("attendance.holidays.name")}>
              <Input required maxLength={150} value={holidayName} onChange={(e) => setHolidayName(e.target.value)} />
            </Field>
            <Field label={t("attendance.holidays.kind")}>
              <FormSelect value={holidayKind} onChange={(e) => setHolidayKind(e.target.value as HolidayKind)}>
                <option value="NATIONAL">{t("enums.holidayKind.NATIONAL")}</option>
                <option value="OPTIONAL">{t("enums.holidayKind.OPTIONAL")}</option>
              </FormSelect>
            </Field>
            <div className="self-end">
              <Button type="submit" disabled={busy}>
                {t("attendance.holidays.add")}
              </Button>
            </div>
          </form>
          {holidays.length === 0 ? (
            <Empty />
          ) : (
            <ul className="m-0 grid list-none gap-2 p-0">
              {holidays.map((holiday) => (
                <li
                  key={holiday.id}
                  className="flex items-center justify-between gap-3 rounded-lg border border-line bg-bg px-3 py-2"
                >
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-medium text-fg">{holiday.name}</span>
                    <span className="text-xs text-muted">{formatDay(holiday.date)}</span>
                  </span>
                  <span className="flex shrink-0 items-center gap-2">
                    <StatusPill kind="holidayKind" value={holiday.kind} />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="size-8"
                      onClick={() => void removeHoliday(holiday)}
                    >
                      <Trash2 aria-hidden="true" className="size-4" />
                      <span className="sr-only">{t("common.delete")}</span>
                    </Button>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}
