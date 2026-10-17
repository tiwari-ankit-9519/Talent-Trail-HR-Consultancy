"use client";

import { useEffect, useState } from "react";
import MonthCalendar from "@/components/attendance/MonthCalendar";
import ExportButton from "@/components/reports/ExportButton";
import { Button } from "@/components/shadcn/button";
import { Input } from "@/components/shadcn/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Empty, ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { currentMonthKey, formatMinutes } from "@/lib/attendance";
import { t } from "@/lib/messages";
import type { AttendanceMonth, AttendanceSummaryRow } from "@/lib/types";
import { MonthField } from "@/components/shadcn/month-picker";

export default function SummaryPanel() {
  const [month, setMonth] = useState(currentMonthKey());
  const [rows, setRows] = useState<AttendanceSummaryRow[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [selected, setSelected] = useState<AttendanceSummaryRow | null>(null);
  const [detail, setDetail] = useState<AttendanceMonth | null>(null);

  useEffect(() => {
    let cancelled = false;
    setRows(null);
    setError(null);
    api<AttendanceSummaryRow[]>("/attendance/summary", { query: { month } })
      .then((result) => {
        if (!cancelled) setRows(result);
      })
      .catch((failure) => {
        if (!cancelled) setError(failure);
      });
    return () => {
      cancelled = true;
    };
  }, [month]);

  useEffect(() => {
    if (!selected) {
      setDetail(null);
      return;
    }
    let cancelled = false;
    api<AttendanceMonth>(`/attendance/users/${selected.userId}/month`, { query: { month } })
      .then((result) => {
        if (!cancelled) setDetail(result);
      })
      .catch((failure) => {
        if (!cancelled) setError(failure);
      });
    return () => {
      cancelled = true;
    };
  }, [selected, month]);

  return (
    <div className="grid gap-4">
      <div className="toolbar">
        <Field label={t("attendance.summary.month")}>
          <MonthField value={month} onChange={(e) => setMonth(e.target.value || currentMonthKey())} />
        </Field>
        <div className="ml-auto self-end">
          <ExportButton
            fileName={`attendance-summary-${month}.csv`}
            disabled={!rows || rows.length === 0}
            headers={[
              t("attendance.team.name"),
              t("attendance.totals.present"),
              t("attendance.totals.halfDay"),
              t("attendance.totals.absent"),
              t("attendance.totals.late"),
              t("attendance.totals.wfh"),
              t("attendance.totals.holidays"),
              t("attendance.totals.weeklyOffs"),
              t("attendance.totals.leaves"),
              t("attendance.totals.worked"),
              t("attendance.summary.payable"),
            ]}
            rows={(rows ?? []).map((row) => [
              row.name,
              row.totals.present,
              row.totals.halfDay,
              row.totals.absent,
              row.totals.late,
              row.totals.wfh,
              row.totals.holidays,
              row.totals.weeklyOffs,
              row.totals.leaves,
              formatMinutes(row.totals.workedMinutes),
              row.totals.payableDays,
            ])}
          />
        </div>
      </div>
      <ErrorBanner error={error} />
      {!rows && !error && <Loading />}
      {rows && rows.length === 0 && <Empty />}
      {rows && rows.length > 0 && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("attendance.team.name")}</TableHead>
                <TableHead>{t("attendance.totals.present")}</TableHead>
                <TableHead>{t("attendance.totals.halfDay")}</TableHead>
                <TableHead>{t("attendance.totals.absent")}</TableHead>
                <TableHead>{t("attendance.totals.late")}</TableHead>
                <TableHead>{t("attendance.totals.wfh")}</TableHead>
                <TableHead>{t("attendance.totals.holidays")}</TableHead>
                <TableHead>{t("attendance.totals.weeklyOffs")}</TableHead>
                <TableHead>{t("attendance.totals.leaves")}</TableHead>
                <TableHead>{t("attendance.totals.worked")}</TableHead>
                <TableHead>{t("attendance.summary.payable")}</TableHead>
                <TableHead>{t("common.actions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.userId}>
                  <TableCell>{row.name}</TableCell>
                  <TableCell>{row.totals.present}</TableCell>
                  <TableCell>{row.totals.halfDay}</TableCell>
                  <TableCell className={row.totals.absent > 0 ? "font-semibold text-bad" : undefined}>
                    {row.totals.absent}
                  </TableCell>
                  <TableCell>{row.totals.late}</TableCell>
                  <TableCell>{row.totals.wfh}</TableCell>
                  <TableCell>{row.totals.holidays}</TableCell>
                  <TableCell>{row.totals.weeklyOffs}</TableCell>
                  <TableCell>{row.totals.leaves}</TableCell>
                  <TableCell>{formatMinutes(row.totals.workedMinutes)}</TableCell>
                  <TableCell className="font-semibold">{row.totals.payableDays}</TableCell>
                  <TableCell>
                    <Button type="button" variant="outline" size="sm" onClick={() => setSelected(row)}>
                      {t("attendance.summary.view")}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      {selected && <MonthCalendar data={detail} monthKey={month} onMonthChange={setMonth} title={selected.name} />}
    </div>
  );
}
