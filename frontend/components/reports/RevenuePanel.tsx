"use client";

import { AlertTriangle, IndianRupee, ReceiptText, Wallet } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import StatCard from "@/components/dashboard/StatCard";
import { compactMoney, fullMoney, monthLabel } from "@/components/dashboard/format";
import ExportButton from "@/components/reports/ExportButton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/shadcn/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { formatMoney, t } from "@/lib/messages";
import type { ReportRevenue } from "@/lib/types";
import { useReport, type ReportFilters } from "@/lib/useReport";

export default function RevenuePanel({ filters }: { filters: ReportFilters }) {
  const { data, error, loading } = useReport<ReportRevenue>("/reports/revenue", filters);
  if (loading) return <Loading />;
  if (error) return <ErrorBanner error={error} />;
  if (!data) return null;
  const chart = data.trend.map((point) => ({
    name: monthLabel(point.month),
    invoiced: point.invoiced,
    collected: point.collected,
  }));
  const buckets = [
    { key: "notDue", label: t("reports.revenue.notDue"), value: data.aging.notDue, className: "bg-ok" },
    { key: "d1", label: t("reports.revenue.days1to30"), value: data.aging.days1to30, className: "bg-warn" },
    { key: "d31", label: t("reports.revenue.days31to60"), value: data.aging.days31to60, className: "bg-warn" },
    { key: "d61", label: t("reports.revenue.days61to90"), value: data.aging.days61to90, className: "bg-bad" },
    { key: "d90", label: t("reports.revenue.days90plus"), value: data.aging.days90plus, className: "bg-bad" },
  ];
  const largest = Math.max(1, ...buckets.map((bucket) => bucket.value));

  return (
    <div className="grid gap-4">
      <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
        <StatCard
          label={t("reports.revenue.invoiced")}
          value={compactMoney(data.invoiced)}
          title={fullMoney(data.invoiced)}
          icon={ReceiptText}
        />
        <StatCard
          label={t("reports.revenue.collected")}
          value={compactMoney(data.collected)}
          title={fullMoney(data.collected)}
          icon={Wallet}
          tone="ok"
        />
        <StatCard
          label={t("reports.revenue.outstanding")}
          value={compactMoney(data.outstanding)}
          title={fullMoney(data.outstanding)}
          hint={t("reports.revenue.asOfToday")}
          icon={IndianRupee}
          tone="warn"
        />
        <StatCard
          label={t("reports.revenue.overdue")}
          value={compactMoney(data.overdue)}
          title={fullMoney(data.overdue)}
          hint={t("reports.revenue.asOfToday")}
          icon={AlertTriangle}
          tone="bad"
        />
      </div>
      <div className="page-grid">
        <Card>
          <CardHeader>
            <CardTitle>{t("reports.revenue.trend")}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chart} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                  <CartesianGrid vertical={false} stroke="var(--border)" />
                  <XAxis dataKey="name" tickLine={false} axisLine={false} stroke="var(--muted)" fontSize={12} />
                  <YAxis
                    tickLine={false}
                    axisLine={false}
                    stroke="var(--muted)"
                    fontSize={12}
                    tickFormatter={(value: number) => compactMoney(value)}
                  />
                  <Tooltip
                    formatter={(value) => fullMoney(Number(value))}
                    cursor={{ fill: "color-mix(in oklab, var(--accent) 8%, transparent)" }}
                    contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 8 }}
                  />
                  <Legend />
                  <Bar
                    dataKey="invoiced"
                    name={t("reports.revenue.invoiced")}
                    fill="var(--accent)"
                    radius={[6, 6, 0, 0]}
                  />
                  <Bar
                    dataKey="collected"
                    name={t("reports.revenue.collected")}
                    fill="var(--ok)"
                    radius={[6, 6, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>{t("reports.revenue.aging")}</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3">
            {buckets.map((bucket) => (
              <div key={bucket.key} className="grid gap-1">
                <div className="flex items-center justify-between gap-3 text-sm">
                  <span>{bucket.label}</span>
                  <strong>{fullMoney(bucket.value)}</strong>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-bg">
                  <div
                    className={`h-full rounded-full transition-all duration-700 ${bucket.className}`}
                    style={{ width: `${(bucket.value / largest) * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
      <div className="flex items-center justify-between gap-3">
        <h2 className="m-0">{t("reports.revenue.byClient")}</h2>
        <ExportButton
          fileName="revenue-by-client.csv"
          disabled={data.rows.length === 0}
          headers={[
            t("reports.revenue.client"),
            t("reports.revenue.invoiced"),
            t("reports.revenue.collected"),
            t("reports.revenue.outstanding"),
            t("reports.revenue.overdue"),
          ]}
          rows={data.rows.map((row) => [row.clientName, row.invoiced, row.collected, row.outstanding, row.overdue])}
        />
      </div>
      {data.rows.length === 0 ? (
        <Empty />
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("reports.revenue.client")}</TableHead>
                <TableHead>{t("reports.revenue.invoiced")}</TableHead>
                <TableHead>{t("reports.revenue.collected")}</TableHead>
                <TableHead>{t("reports.revenue.outstanding")}</TableHead>
                <TableHead>{t("reports.revenue.overdue")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.rows.map((row) => (
                <TableRow key={row.clientId}>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>{formatMoney(row.invoiced)}</TableCell>
                  <TableCell>{formatMoney(row.collected)}</TableCell>
                  <TableCell>{formatMoney(row.outstanding)}</TableCell>
                  <TableCell className={row.overdue > 0 ? "font-semibold text-bad" : undefined}>
                    {formatMoney(row.overdue)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
