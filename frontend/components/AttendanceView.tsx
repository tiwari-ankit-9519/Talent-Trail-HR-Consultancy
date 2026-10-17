"use client";

import { useCallback, useEffect, useState } from "react";
import CorrectionForm from "@/components/attendance/CorrectionForm";
import LeavePanel from "@/components/attendance/LeavePanel";
import MonthCalendar from "@/components/attendance/MonthCalendar";
import MyRequests from "@/components/attendance/MyRequests";
import TodayCard from "@/components/attendance/TodayCard";
import { Button } from "@/components/shadcn/button";
import { ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { currentMonthKey } from "@/lib/attendance";
import { t } from "@/lib/messages";
import type { AttendanceCorrection, AttendanceDay, AttendanceMonth, AttendanceToday } from "@/lib/types";

export default function AttendanceView() {
  const [today, setToday] = useState<AttendanceToday | null>(null);
  const [monthKey, setMonthKey] = useState(currentMonthKey());
  const [month, setMonth] = useState<AttendanceMonth | null>(null);
  const [requests, setRequests] = useState<AttendanceCorrection[]>([]);
  const [presetDate, setPresetDate] = useState("");
  const [showCorrection, setShowCorrection] = useState(false);
  const [leaveKey, setLeaveKey] = useState(0);
  const [error, setError] = useState<unknown>(null);

  const loadToday = useCallback(async () => {
    try {
      setToday(await api<AttendanceToday>("/attendance/today"));
    } catch (failure) {
      setError(failure);
    }
  }, []);

  const loadMonth = useCallback(async () => {
    try {
      setError(null);
      setMonth(await api<AttendanceMonth>("/attendance/me", { query: { month: monthKey } }));
    } catch (failure) {
      setError(failure);
    }
  }, [monthKey]);

  const loadRequests = useCallback(async () => {
    try {
      setRequests(await api<AttendanceCorrection[]>("/attendance/requests/mine"));
    } catch (failure) {
      setError(failure);
    }
  }, []);

  useEffect(() => {
    void loadToday();
    void loadRequests();
  }, [loadToday, loadRequests]);

  useEffect(() => {
    void loadMonth();
  }, [loadMonth]);

  function pick(day: AttendanceDay) {
    setPresetDate(day.date);
    setShowCorrection(true);
  }

  return (
    <section>
      <div className="page-head flex flex-wrap items-center justify-between gap-3">
        <h1>{t("attendance.heading")}</h1>
        <Button type="button" variant="outline" onClick={() => setShowCorrection((value) => !value)}>
          {t("attendance.correction.open")}
        </Button>
      </div>
      <ErrorBanner error={error} />
      {!today && !error && <Loading />}
      <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)]">
        {today && (
          <TodayCard
            today={today}
            onChange={(next) => {
              setToday(next);
              void loadMonth();
              setLeaveKey((value) => value + 1);
            }}
          />
        )}
        <LeavePanel
          refreshKey={leaveKey}
          onChanged={() => {
            void loadMonth();
            void loadToday();
          }}
        />
      </div>
      {showCorrection && (
        <div className="mt-4">
          <CorrectionForm
            presetDate={presetDate}
            onCreated={() => {
              void loadRequests();
              void loadMonth();
            }}
          />
        </div>
      )}
      <div className="mt-4">
        <MonthCalendar data={month} monthKey={monthKey} onMonthChange={setMonthKey} onPick={pick} />
      </div>
      <MyRequests rows={requests} />
    </section>
  );
}
