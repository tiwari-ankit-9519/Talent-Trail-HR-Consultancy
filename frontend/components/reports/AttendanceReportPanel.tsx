"use client";

import { useState } from "react";
import { MonthField } from "@/components/shadcn/month-picker";
import DataTable, { type Column } from "@/components/reports/DataTable";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { currentMonthKey, formatMinutes } from "@/lib/attendance";
import { t } from "@/lib/messages";
import type { AttendanceSummaryRow } from "@/lib/types";
import { useFetch } from "@/lib/useFetch";

export default function AttendanceReportPanel() {
  const [month, setMonth] = useState(currentMonthKey());
  const { data, error, loading } = useFetch<AttendanceSummaryRow[]>("/attendance/summary", { month });

  const columns: Column<AttendanceSummaryRow>[] = [
    { header: t("reports.attendance.employee"), cell: (r) => r.name, value: (r) => r.name },
    {
      header: t("reports.attendance.present"),
      cell: (r) => r.totals.present,
      value: (r) => r.totals.present,
      numeric: true,
    },
    {
      header: t("reports.attendance.halfDay"),
      cell: (r) => r.totals.halfDay,
      value: (r) => r.totals.halfDay,
      numeric: true,
    },
    {
      header: t("reports.attendance.absent"),
      cell: (r) => r.totals.absent,
      value: (r) => r.totals.absent,
      numeric: true,
    },
    { header: t("reports.attendance.late"), cell: (r) => r.totals.late, value: (r) => r.totals.late, numeric: true },
    { header: t("reports.attendance.wfh"), cell: (r) => r.totals.wfh, value: (r) => r.totals.wfh, numeric: true },
    {
      header: t("reports.attendance.leaves"),
      cell: (r) => r.totals.leaves,
      value: (r) => r.totals.leaves,
      numeric: true,
    },
    {
      header: t("reports.attendance.lop"),
      cell: (r) => r.totals.lossOfPay,
      value: (r) => r.totals.lossOfPay,
      numeric: true,
    },
    {
      header: t("reports.attendance.holidays"),
      cell: (r) => r.totals.holidays,
      value: (r) => r.totals.holidays,
      numeric: true,
    },
    {
      header: t("reports.attendance.hours"),
      cell: (r) => formatMinutes(r.totals.workedMinutes),
      value: (r) => Math.round((r.totals.workedMinutes / 60) * 100) / 100,
      numeric: true,
    },
    {
      header: t("reports.attendance.payable"),
      cell: (r) => <strong>{r.totals.payableDays}</strong>,
      value: (r) => r.totals.payableDays,
      numeric: true,
    },
  ];

  return (
    <div className="grid gap-4">
      <div className="form-grid">
        <Field label={t("reports.month")}>
          <MonthField value={month} onChange={(e) => setMonth(e.target.value)} />
        </Field>
      </div>
      {loading && <Loading />}
      {error ? <ErrorBanner error={error} /> : null}
      {data && (
        <DataTable
          title={t("reports.attendance.title")}
          fileName={`attendance-${month}.xlsx`}
          columns={columns}
          rows={data}
          rowKey={(r) => r.userId}
        />
      )}
    </div>
  );
}
