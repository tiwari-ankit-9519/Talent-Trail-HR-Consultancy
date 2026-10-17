"use client";

import { useState } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Badge } from "@/components/shadcn/badge";
import { MonthField } from "@/components/shadcn/month-picker";
import DataTable, { type Column } from "@/components/reports/DataTable";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { currentMonthKey } from "@/lib/attendance";
import { formatMoney, t } from "@/lib/messages";
import type { PayrollRegisterReport, PayrollRegisterRow } from "@/lib/types";
import { useFetch } from "@/lib/useFetch";

export default function PayrollRegisterPanel() {
  const [month, setMonth] = useState(currentMonthKey());
  const { data, error, loading } = useFetch<PayrollRegisterReport>("/reports/payroll-register", { month });

  const columns: Column<PayrollRegisterRow>[] = [
    { header: t("reports.payroll.employee"), cell: (r) => r.name, value: (r) => r.name },
    { header: t("reports.payroll.code"), cell: (r) => r.employeeCode ?? "", value: (r) => r.employeeCode },
    { header: t("reports.payroll.designation"), cell: (r) => r.designation ?? "", value: (r) => r.designation },
    {
      header: t("reports.payroll.payableDays"),
      cell: (r) => r.payableDays,
      value: (r) => r.payableDays,
      numeric: true,
    },
    { header: t("reports.payroll.lop"), cell: (r) => r.lossOfPayDays, value: (r) => r.lossOfPayDays, numeric: true },
    {
      header: t("reports.payroll.earned"),
      cell: (r) => formatMoney(r.earnedGross),
      value: (r) => r.earnedGross,
      numeric: true,
    },
    {
      header: t("reports.payroll.commission"),
      cell: (r) => formatMoney(r.commission),
      value: (r) => r.commission,
      numeric: true,
    },
    {
      header: t("reports.payroll.otherEarnings"),
      cell: (r) => formatMoney(r.otherEarnings),
      value: (r) => r.otherEarnings,
      numeric: true,
    },
    { header: t("reports.payroll.pf"), cell: (r) => formatMoney(r.pf), value: (r) => r.pf, numeric: true },
    { header: t("reports.payroll.pt"), cell: (r) => formatMoney(r.pt), value: (r) => r.pt, numeric: true },
    { header: t("reports.payroll.tds"), cell: (r) => formatMoney(r.tds), value: (r) => r.tds, numeric: true },
    {
      header: t("reports.payroll.otherDeductions"),
      cell: (r) => formatMoney(r.otherDeductions),
      value: (r) => r.otherDeductions,
      numeric: true,
    },
    {
      header: t("reports.payroll.net"),
      cell: (r) => <strong>{formatMoney(r.net)}</strong>,
      value: (r) => r.net,
      numeric: true,
    },
  ];
  if (data?.bankIncluded) {
    columns.push(
      {
        header: t("reports.payroll.accountHolder"),
        cell: (r) => r.bankAccountHolder ?? "",
        value: (r) => r.bankAccountHolder,
      },
      { header: t("reports.payroll.bank"), cell: (r) => r.bankName ?? "", value: (r) => r.bankName },
      { header: t("reports.payroll.ifsc"), cell: (r) => r.bankIfsc ?? "", value: (r) => r.bankIfsc },
      {
        header: t("reports.payroll.account"),
        cell: (r) => r.bankAccountNumber ?? "",
        value: (r) => r.bankAccountNumber,
      },
      { header: t("reports.payroll.pan"), cell: (r) => r.pan ?? "", value: (r) => r.pan },
    );
  }

  return (
    <div className="grid gap-4">
      <div className="form-grid">
        <Field label={t("reports.month")}>
          <MonthField value={month} onChange={(e) => setMonth(e.target.value)} />
        </Field>
      </div>
      {loading && <Loading />}
      {error ? <ErrorBanner error={error} /> : null}
      {data && data.status === "NONE" && <Alert variant="info">{t("reports.payroll.noRun")}</Alert>}
      {data && data.status !== "NONE" && (
        <>
          <div className="flex flex-wrap items-center gap-3">
            <Badge variant={data.status === "FINALIZED" ? "ok" : "warn"}>
              {data.status === "FINALIZED" ? t("reports.payroll.finalized") : t("reports.payroll.draft")}
            </Badge>
            <span className="muted">
              {t("reports.payroll.netTotal")}: <strong>{formatMoney(data.totals.net)}</strong>
            </span>
          </div>
          <DataTable
            title={t("reports.payroll.title")}
            fileName={`payroll-register-${data.month}.xlsx`}
            columns={columns}
            rows={data.rows}
            rowKey={(r) => r.userId}
          />
        </>
      )}
    </div>
  );
}
