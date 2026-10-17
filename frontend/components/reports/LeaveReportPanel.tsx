"use client";

import { useState } from "react";
import { FormSelect } from "@/components/shadcn/select";
import DataTable, { type Column } from "@/components/reports/DataTable";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { t } from "@/lib/messages";
import type { LeaveReport, LeaveReportRow } from "@/lib/types";
import { useFetch } from "@/lib/useFetch";

export default function LeaveReportPanel() {
  const current = new Date().getFullYear();
  const [year, setYear] = useState(current);
  const { data, error, loading } = useFetch<LeaveReport>("/reports/leave", { year });

  const columns: Column<LeaveReportRow>[] = [
    { header: t("reports.leave.employee"), cell: (r) => r.name, value: (r) => r.name },
  ];
  data?.types.forEach((type, index) => {
    columns.push({
      header: `${type.name} · ${t("reports.leave.approved")}`,
      cell: (r) => r.cells[index]?.approved ?? 0,
      value: (r) => r.cells[index]?.approved ?? 0,
      numeric: true,
    });
    columns.push({
      header: `${type.name} · ${t("reports.leave.pending")}`,
      cell: (r) => r.cells[index]?.pending ?? 0,
      value: (r) => r.cells[index]?.pending ?? 0,
      numeric: true,
    });
    if (!type.unpaid) {
      columns.push({
        header: `${type.name} · ${t("reports.leave.remaining")}`,
        cell: (r) => r.cells[index]?.remaining ?? "",
        value: (r) => r.cells[index]?.remaining ?? null,
        numeric: true,
      });
    }
  });
  columns.push({
    header: t("reports.leave.totalApproved"),
    cell: (r) => <strong>{r.totalApproved}</strong>,
    value: (r) => r.totalApproved,
    numeric: true,
  });

  return (
    <div className="grid gap-4">
      <div className="form-grid">
        <Field label={t("reports.leave.year")}>
          <FormSelect value={String(year)} onChange={(e) => setYear(Number(e.target.value))}>
            {[current, current - 1, current - 2].map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </FormSelect>
        </Field>
      </div>
      {loading && <Loading />}
      {error ? <ErrorBanner error={error} /> : null}
      {data && (
        <DataTable
          title={t("reports.leave.title")}
          fileName={`leave-${data.year}.xlsx`}
          columns={columns}
          rows={data.rows}
          rowKey={(r) => r.userId}
        />
      )}
    </div>
  );
}
