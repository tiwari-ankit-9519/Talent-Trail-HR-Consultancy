"use client";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/shadcn/card";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { t, tx } from "@/lib/messages";
import type { ReportFunnel } from "@/lib/types";
import { useReport, type ReportFilters } from "@/lib/useReport";

export default function FunnelPanel({ filters }: { filters: ReportFilters }) {
  const { data, error, loading } = useReport<ReportFunnel>("/reports/funnel", filters);
  if (loading) return <Loading />;
  if (error) return <ErrorBanner error={error} />;
  if (!data) return null;
  const first = data.stages[0]?.count ?? 0;
  if (first === 0) return <Empty />;

  return (
    <div className="page-grid">
      <Card className="wide">
        <CardHeader>
          <CardTitle>{t("reports.funnel.heading")}</CardTitle>
          <CardDescription>{t("reports.funnel.hint")}</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4">
          {data.stages.map((stage, index) => {
            const width = Math.max(2, (stage.count / first) * 100);
            const previous = index === 0 ? stage.count : data.stages[index - 1].count;
            const step = previous === 0 ? "—" : `${Math.round((stage.count / previous) * 100)}%`;
            const overall = `${Math.round((stage.count / first) * 100)}%`;
            return (
              <div key={stage.key} className="grid gap-1.5">
                <div className="flex flex-wrap items-baseline justify-between gap-2 text-sm">
                  <span className="font-medium">{tx(`reports.funnel.stages.${stage.key}`, stage.key)}</span>
                  <span className="muted">
                    <strong className="text-fg">{stage.count}</strong>
                    {index > 0 &&
                      ` · ${step} ${t("reports.funnel.ofPrevious")} · ${overall} ${t("reports.funnel.ofSubmitted")}`}
                  </span>
                </div>
                <div className="h-3 overflow-hidden rounded-full bg-bg">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-accent to-[color-mix(in_oklab,var(--accent)_55%,#7c3aed)] transition-all duration-700"
                    style={{ width: `${width}%` }}
                  />
                </div>
              </div>
            );
          })}
          {data.averageDaysToJoin !== null && (
            <p className="muted small m-0">
              {t("reports.funnel.averageDays")}:{" "}
              <strong className="text-fg">{Math.round(data.averageDaysToJoin)}</strong> {t("reports.funnel.days")}
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
