"use client";

import ExportButton from "@/components/reports/ExportButton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { formatMoney, t } from "@/lib/messages";
import type { ReportRecruiterRow } from "@/lib/types";
import { useReport, type ReportFilters } from "@/lib/useReport";

function rate(joined: number, submissions: number): string {
  if (submissions === 0) return "—";
  return `${Math.round((joined / submissions) * 1000) / 10}%`;
}

export default function RecruitersPanel({ filters }: { filters: ReportFilters }) {
  const { data, error, loading } = useReport<ReportRecruiterRow[]>("/reports/recruiters", filters);
  if (loading) return <Loading />;
  if (error) return <ErrorBanner error={error} />;
  if (!data) return null;
  const showMoney = data.some((row) => row.commissionTotal !== null);

  return (
    <div className="grid gap-4">
      <div className="flex items-center justify-between gap-3">
        <p className="muted small m-0">{t("reports.recruiters.hint")}</p>
        <ExportButton
          fileName="recruiters.csv"
          disabled={data.length === 0}
          headers={[
            t("reports.recruiters.recruiter"),
            t("reports.recruiters.submissions"),
            t("reports.recruiters.selected"),
            t("reports.recruiters.rejected"),
            t("reports.recruiters.joined"),
            t("reports.recruiters.conversion"),
            t("reports.recruiters.commission"),
            t("reports.recruiters.commissionPaid"),
          ]}
          rows={data.map((row) => [
            row.name ?? row.email,
            row.submissions,
            row.selected,
            row.rejected,
            row.joined,
            rate(row.joined, row.submissions),
            row.commissionTotal,
            row.commissionPaid,
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
                <TableHead>{t("reports.recruiters.recruiter")}</TableHead>
                <TableHead>{t("reports.recruiters.submissions")}</TableHead>
                <TableHead>{t("reports.recruiters.selected")}</TableHead>
                <TableHead>{t("reports.recruiters.rejected")}</TableHead>
                <TableHead>{t("reports.recruiters.joined")}</TableHead>
                <TableHead>{t("reports.recruiters.conversion")}</TableHead>
                {showMoney && <TableHead>{t("reports.recruiters.commission")}</TableHead>}
                {showMoney && <TableHead>{t("reports.recruiters.commissionPaid")}</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.map((row) => (
                <TableRow key={row.email ?? "unknown"}>
                  <TableCell>
                    <div className="grid">
                      <span className="font-medium">{row.name ?? row.email ?? "—"}</span>
                      {row.name && row.email && <span className="muted small">{row.email}</span>}
                    </div>
                  </TableCell>
                  <TableCell>{row.submissions}</TableCell>
                  <TableCell>{row.selected}</TableCell>
                  <TableCell>{row.rejected}</TableCell>
                  <TableCell className="font-semibold">{row.joined}</TableCell>
                  <TableCell>{rate(row.joined, row.submissions)}</TableCell>
                  {showMoney && <TableCell>{formatMoney(row.commissionTotal)}</TableCell>}
                  {showMoney && <TableCell>{formatMoney(row.commissionPaid)}</TableCell>}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
