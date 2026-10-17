"use client";

import { useState } from "react";
import { StatCard } from "@/components/reports/StatRow";
import { Badge } from "@/components/shadcn/badge";
import { FormSelect } from "@/components/shadcn/select";
import DataTable, { type Column } from "@/components/reports/DataTable";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { formatDate, formatMoney, t } from "@/lib/messages";
import type { CommissionReportRow, CommissionsReport } from "@/lib/types";
import { useFetch } from "@/lib/useFetch";
import type { ReportFilters } from "@/lib/useReport";

const stateLabel: Record<CommissionReportRow["state"], string> = {
  PAYABLE: t("reports.commissions.states.payable"),
  PAID: t("reports.commissions.states.paid"),
  CANCELLED: t("reports.commissions.states.cancelled"),
  AMOUNT_NOT_SET: t("reports.commissions.states.amountNotSet"),
  PERIOD_RUNNING: t("reports.commissions.states.periodRunning"),
  INVOICE_UNPAID: t("reports.commissions.states.invoiceUnpaid"),
};

export default function CommissionsReportPanel({ filters }: { filters: ReportFilters }) {
  const [status, setStatus] = useState("");
  const { data, error, loading } = useFetch<CommissionsReport>("/reports/commissions", {
    from: filters.from || undefined,
    to: filters.to || undefined,
    status: status || undefined,
  });

  const columns: Column<CommissionReportRow>[] = [
    { header: t("reports.commissions.recruiter"), cell: (r) => r.recruiter, value: (r) => r.recruiter },
    { header: t("reports.commissions.candidate"), cell: (r) => r.candidateName, value: (r) => r.candidateName },
    { header: t("reports.commissions.client"), cell: (r) => r.clientName, value: (r) => r.clientName },
    { header: t("reports.commissions.job"), cell: (r) => r.jobCode, value: (r) => r.jobCode },
    { header: t("reports.commissions.joined"), cell: (r) => formatDate(r.joiningDate), value: (r) => r.joiningDate },
    {
      header: t("reports.commissions.amount"),
      cell: (r) => formatMoney(r.amount),
      value: (r) => r.amount,
      numeric: true,
    },
    {
      header: t("reports.commissions.state"),
      cell: (r) => (
        <Badge
          variant={
            r.state === "PAYABLE" ? "accent" : r.state === "PAID" ? "ok" : r.state === "CANCELLED" ? "default" : "warn"
          }
        >
          {stateLabel[r.state]}
        </Badge>
      ),
      value: (r) => stateLabel[r.state],
    },
    {
      header: t("reports.commissions.payableAfter"),
      cell: (r) => formatDate(r.payableAfter),
      value: (r) => r.payableAfter,
    },
    { header: t("reports.commissions.invoice"), cell: (r) => r.invoiceNumber ?? "", value: (r) => r.invoiceNumber },
    { header: t("reports.commissions.paidOn"), cell: (r) => formatDate(r.paidOn), value: (r) => r.paidOn },
    {
      header: t("reports.commissions.reference"),
      cell: (r) => r.paymentReference ?? "",
      value: (r) => r.paymentReference,
    },
  ];

  return (
    <div className="grid gap-4">
      <div className="form-grid">
        <Field label={t("reports.commissions.statusFilter")}>
          <FormSelect value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">{t("reports.commissions.allStatuses")}</option>
            <option value="PENDING">{t("reports.commissions.pending")}</option>
            <option value="PAID">{t("reports.commissions.states.paid")}</option>
            <option value="CANCELLED">{t("reports.commissions.states.cancelled")}</option>
          </FormSelect>
        </Field>
      </div>
      {loading && <Loading />}
      {error ? <ErrorBanner error={error} /> : null}
      {data && (
        <>
          <div className="grid grid-cols-2 gap-3 xl:grid-cols-3">
            <StatCard label={t("reports.commissions.payableNow")} value={formatMoney(data.payable)} />
            <StatCard label={t("reports.commissions.waiting")} value={formatMoney(data.waiting)} />
            <StatCard label={t("reports.commissions.paidTotal")} value={formatMoney(data.paid)} />
          </div>
          <DataTable
            title={t("reports.commissions.byRecruiter")}
            fileName="commissions-by-recruiter.xlsx"
            columns={[
              { header: t("reports.commissions.recruiter"), cell: (r) => r.recruiter, value: (r) => r.recruiter },
              { header: t("reports.commissions.count"), cell: (r) => r.count, value: (r) => r.count, numeric: true },
              {
                header: t("reports.commissions.payableNow"),
                cell: (r) => formatMoney(r.payable),
                value: (r) => r.payable,
                numeric: true,
              },
              {
                header: t("reports.commissions.waiting"),
                cell: (r) => formatMoney(r.waiting),
                value: (r) => r.waiting,
                numeric: true,
              },
              {
                header: t("reports.commissions.paidTotal"),
                cell: (r) => formatMoney(r.paid),
                value: (r) => r.paid,
                numeric: true,
              },
            ]}
            rows={data.byRecruiter}
            rowKey={(r) => r.recruiter}
          />
          <DataTable
            title={t("reports.commissions.title")}
            fileName="commissions.xlsx"
            columns={columns}
            rows={data.rows}
            rowKey={(r) => r.commissionId}
          />
        </>
      )}
    </div>
  );
}
