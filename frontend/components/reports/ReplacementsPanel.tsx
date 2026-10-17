"use client";

import ExportButton from "@/components/reports/ExportButton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { t } from "@/lib/messages";
import type { ReportReplacementRow } from "@/lib/types";
import { useReport, type ReportFilters } from "@/lib/useReport";

function rate(exits: number, joined: number): string {
  if (joined === 0) return "—";
  return `${Math.round((exits / joined) * 1000) / 10}%`;
}

export default function ReplacementsPanel({ filters }: { filters: ReportFilters }) {
  const { data, error, loading } = useReport<ReportReplacementRow[]>("/reports/replacements", filters);
  if (loading) return <Loading />;
  if (error) return <ErrorBanner error={error} />;
  if (!data) return null;

  return (
    <div className="grid gap-4">
      <div className="flex items-center justify-between gap-3">
        <p className="muted small m-0">{t("reports.replacements.hint")}</p>
        <ExportButton
          fileName="replacements.csv"
          disabled={data.length === 0}
          headers={[
            t("reports.replacements.client"),
            t("reports.replacements.joined"),
            t("reports.replacements.exits"),
            t("reports.replacements.withinPeriod"),
            t("reports.replacements.filled"),
            t("reports.replacements.open"),
            t("reports.replacements.exitRate"),
          ]}
          rows={data.map((row) => [
            row.clientName,
            row.joined,
            row.exits,
            row.withinPeriod,
            row.filled,
            row.open,
            rate(row.exits, row.joined),
          ])}
        />
      </div>
      {data.length === 0 ? (
        <Empty />
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("reports.replacements.client")}</TableHead>
                <TableHead>{t("reports.replacements.joined")}</TableHead>
                <TableHead>{t("reports.replacements.exits")}</TableHead>
                <TableHead>{t("reports.replacements.withinPeriod")}</TableHead>
                <TableHead>{t("reports.replacements.filled")}</TableHead>
                <TableHead>{t("reports.replacements.open")}</TableHead>
                <TableHead>{t("reports.replacements.exitRate")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.map((row) => (
                <TableRow key={row.clientId}>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>{row.joined}</TableCell>
                  <TableCell>{row.exits}</TableCell>
                  <TableCell>{row.withinPeriod}</TableCell>
                  <TableCell>{row.filled}</TableCell>
                  <TableCell>{row.open}</TableCell>
                  <TableCell className={row.exits > 0 ? "font-semibold text-warn" : undefined}>
                    {rate(row.exits, row.joined)}
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
