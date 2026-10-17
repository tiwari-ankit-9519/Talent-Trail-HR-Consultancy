"use client";

import DataTable, { type Column } from "@/components/reports/DataTable";
import { ErrorBanner, Loading } from "@/components/ui";
import { monthLabel } from "@/components/dashboard/format";
import { formatMoney, t } from "@/lib/messages";
import type { TaxMonthRow, TaxSummaryReport } from "@/lib/types";
import { useFetch } from "@/lib/useFetch";
import type { ReportFilters } from "@/lib/useReport";

export default function TaxPanel({ filters }: { filters: ReportFilters }) {
  const { data, error, loading } = useFetch<TaxSummaryReport>("/reports/tax-summary", {
    from: filters.from || undefined,
    to: filters.to || undefined,
    clientId: filters.clientId || undefined,
  });
  if (loading) return <Loading />;
  if (error) return <ErrorBanner error={error} />;
  if (!data) return null;

  const money = (header: string, pick: (r: TaxMonthRow) => number): Column<TaxMonthRow> => ({
    header,
    cell: (r) => formatMoney(pick(r)),
    value: pick,
    numeric: true,
  });
  const columns: Column<TaxMonthRow>[] = [
    {
      header: t("reports.tax.month"),
      cell: (r) => (r.month ? monthLabel(r.month) : t("reports.tax.total")),
      value: (r) => r.month || t("reports.tax.total"),
    },
    money(t("reports.tax.base"), (r) => r.baseAmount),
    money(t("reports.tax.cgst"), (r) => r.cgst),
    money(t("reports.tax.sgst"), (r) => r.sgst),
    money(t("reports.tax.igst"), (r) => r.igst),
    money(t("reports.tax.invoiced"), (r) => r.total),
    money(t("reports.tax.tdsExpected"), (r) => r.tdsExpected),
    money(t("reports.tax.tdsDeducted"), (r) => r.tdsDeducted),
    money(t("reports.tax.received"), (r) => r.received),
  ];

  return (
    <DataTable
      title={t("reports.tax.title")}
      fileName="gst-tds-summary.xlsx"
      columns={columns}
      rows={data.months.length ? [...data.months, data.totals] : []}
      rowKey={(r) => r.month || "total"}
    />
  );
}
