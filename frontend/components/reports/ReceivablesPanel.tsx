"use client";

import { StatCard } from "@/components/reports/StatRow";
import { Badge } from "@/components/shadcn/badge";
import DataTable, { type Column } from "@/components/reports/DataTable";
import { ErrorBanner, Loading } from "@/components/ui";
import { formatDate, formatMoney, t } from "@/lib/messages";
import type { ReceivableRow, ReceivablesReport } from "@/lib/types";
import { useFetch } from "@/lib/useFetch";
import type { ReportFilters } from "@/lib/useReport";

const bucketLabel: Record<ReceivableRow["bucket"], ReturnType<typeof t>> = {
  NOT_DUE: t("reports.receivables.notDue"),
  D1_30: t("reports.receivables.days1to30"),
  D31_60: t("reports.receivables.days31to60"),
  D61_90: t("reports.receivables.days61to90"),
  D90_PLUS: t("reports.receivables.days90plus"),
};

export default function ReceivablesPanel({ filters }: { filters: ReportFilters }) {
  const { data, error, loading } = useFetch<ReceivablesReport>("/reports/receivables", {
    clientId: filters.clientId || undefined,
  });
  if (loading) return <Loading />;
  if (error) return <ErrorBanner error={error} />;
  if (!data) return null;

  const columns: Column<ReceivableRow>[] = [
    { header: t("reports.receivables.invoice"), cell: (r) => r.number, value: (r) => r.number },
    { header: t("reports.receivables.client"), cell: (r) => r.clientName, value: (r) => r.clientName },
    { header: t("reports.receivables.candidate"), cell: (r) => r.candidateName, value: (r) => r.candidateName },
    {
      header: t("reports.receivables.invoiceDate"),
      cell: (r) => formatDate(r.invoiceDate),
      value: (r) => r.invoiceDate,
    },
    { header: t("reports.receivables.dueDate"), cell: (r) => formatDate(r.dueDate), value: (r) => r.dueDate },
    { header: t("reports.receivables.total"), cell: (r) => formatMoney(r.total), value: (r) => r.total, numeric: true },
    {
      header: t("reports.receivables.settled"),
      cell: (r) => formatMoney(r.settled),
      value: (r) => r.settled,
      numeric: true,
    },
    {
      header: t("reports.receivables.outstanding"),
      cell: (r) => <strong>{formatMoney(r.outstanding)}</strong>,
      value: (r) => r.outstanding,
      numeric: true,
    },
    {
      header: t("reports.receivables.daysOverdue"),
      cell: (r) => r.daysOverdue,
      value: (r) => r.daysOverdue,
      numeric: true,
    },
    {
      header: t("reports.receivables.bucket"),
      cell: (r) => (
        <Badge variant={r.bucket === "NOT_DUE" ? "ok" : r.bucket === "D1_30" || r.bucket === "D31_60" ? "warn" : "bad"}>
          {bucketLabel[r.bucket]}
        </Badge>
      ),
      value: (r) => bucketLabel[r.bucket],
    },
  ];

  return (
    <div className="grid gap-4">
      <div className="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <StatCard label={t("reports.receivables.outstandingTotal")} value={formatMoney(data.outstanding)} />
        <StatCard label={t("reports.receivables.overdueTotal")} value={formatMoney(data.overdue)} />
        <StatCard label={t("reports.receivables.days1to30")} value={formatMoney(data.aging.days1to30)} />
        <StatCard
          label={t("reports.receivables.days90plus")}
          value={formatMoney(data.aging.days61to90 + data.aging.days90plus)}
          hint={t("reports.receivables.over60")}
        />
      </div>
      <DataTable
        title={t("reports.receivables.title")}
        fileName={`receivables-${data.asOf}.xlsx`}
        columns={columns}
        rows={data.rows}
        rowKey={(r) => r.invoiceId}
      />
    </div>
  );
}
