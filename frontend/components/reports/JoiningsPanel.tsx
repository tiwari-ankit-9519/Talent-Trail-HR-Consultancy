"use client";

import Link from "next/link";
import { Handshake, IndianRupee, ReceiptText } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import StatCard from "@/components/dashboard/StatCard";
import { compactMoney, fullMoney, monthLabel } from "@/components/dashboard/format";
import ExportButton from "@/components/reports/ExportButton";
import StatusPill from "@/components/StatusPill";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/shadcn/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { formatDay, formatMoney, t } from "@/lib/messages";
import type { ReportJoinings } from "@/lib/types";
import { useReport, type ReportFilters } from "@/lib/useReport";

export default function JoiningsPanel({ filters }: { filters: ReportFilters }) {
  const { data, error, loading } = useReport<ReportJoinings>("/reports/joinings", filters);
  if (loading) return <Loading />;
  if (error) return <ErrorBanner error={error} />;
  if (!data) return null;
  const chart = data.byMonth.map((point) => ({ name: monthLabel(point.month), total: point.total }));
  const showMoney = data.totalInvoiced !== null;

  return (
    <div className="grid gap-4">
      <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-3">
        <StatCard label={t("reports.joinings.count")} value={String(data.count)} icon={Handshake} />
        <StatCard
          label={t("reports.joinings.totalCtc")}
          value={compactMoney(data.totalCtc)}
          title={fullMoney(data.totalCtc)}
          icon={IndianRupee}
          tone="ok"
        />
        {showMoney && (
          <StatCard
            label={t("reports.joinings.totalInvoiced")}
            value={compactMoney(data.totalInvoiced ?? 0)}
            title={fullMoney(data.totalInvoiced ?? 0)}
            icon={ReceiptText}
            tone="warn"
          />
        )}
      </div>
      <Card>
        <CardHeader>
          <CardTitle>{t("reports.joinings.byMonth")}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-56 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chart} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                <CartesianGrid vertical={false} stroke="var(--border)" />
                <XAxis dataKey="name" tickLine={false} axisLine={false} stroke="var(--muted)" fontSize={12} />
                <YAxis allowDecimals={false} tickLine={false} axisLine={false} stroke="var(--muted)" fontSize={12} />
                <Tooltip
                  cursor={{ fill: "color-mix(in oklab, var(--accent) 8%, transparent)" }}
                  contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 8 }}
                />
                <Bar dataKey="total" fill="var(--accent)" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>
      <div className="flex items-center justify-between gap-3">
        <h2 className="m-0">{t("reports.joinings.rows")}</h2>
        <ExportButton
          fileName="joinings.csv"
          disabled={data.rows.length === 0}
          headers={[
            t("reports.joinings.joiningDate"),
            t("reports.joinings.candidate"),
            t("reports.joinings.client"),
            t("reports.joinings.job"),
            t("reports.joinings.recruiter"),
            t("reports.joinings.ctc"),
            t("reports.joinings.invoice"),
            t("reports.joinings.invoiceTotal"),
          ]}
          rows={data.rows.map((row) => [
            row.joiningDate,
            row.candidateName,
            row.clientName,
            `${row.jobCode} ${row.jobTitle}`,
            row.recruiter,
            row.offeredCtc,
            row.invoiceNumber,
            row.invoiceTotal,
          ])}
        />
      </div>
      {data.rows.length === 0 ? (
        <Empty />
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("reports.joinings.joiningDate")}</TableHead>
                <TableHead>{t("reports.joinings.candidate")}</TableHead>
                <TableHead>{t("reports.joinings.client")}</TableHead>
                <TableHead>{t("reports.joinings.job")}</TableHead>
                <TableHead>{t("reports.joinings.recruiter")}</TableHead>
                <TableHead>{t("reports.joinings.ctc")}</TableHead>
                {showMoney && <TableHead>{t("reports.joinings.invoice")}</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.rows.map((row) => (
                <TableRow key={row.placementId}>
                  <TableCell>{formatDay(row.joiningDate)}</TableCell>
                  <TableCell>
                    <Link href={`/placements/${row.placementId}`}>{row.candidateName}</Link>
                  </TableCell>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>
                    {row.jobCode} · {row.jobTitle}
                  </TableCell>
                  <TableCell>{row.recruiter ?? "—"}</TableCell>
                  <TableCell>{formatMoney(row.offeredCtc)}</TableCell>
                  {showMoney && (
                    <TableCell>
                      {row.invoiceNumber ? (
                        <div className="grid gap-1">
                          <span>
                            {row.invoiceNumber} · {formatMoney(row.invoiceTotal)}
                          </span>
                          <StatusPill kind="invoiceStatus" value={row.invoiceStatus} />
                        </div>
                      ) : (
                        <span className="muted">{t("reports.joinings.notInvoiced")}</span>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
