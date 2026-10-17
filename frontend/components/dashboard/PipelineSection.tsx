"use client";

import Link from "next/link";
import { Briefcase, CalendarClock, Handshake, Hourglass, UserCheck } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import StatCard from "@/components/dashboard/StatCard";
import { monthLabel } from "@/components/dashboard/format";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/shadcn/card";
import { t, tx } from "@/lib/messages";
import type { DashboardPipeline } from "@/lib/types";

const barColor: Record<string, string> = {
  SUBMITTED: "var(--accent)",
  SHORTLISTED: "var(--accent)",
  INTERVIEWING: "var(--warn)",
  SELECTED: "var(--ok)",
  ON_HOLD: "var(--muted)",
  REJECTED: "var(--bad)",
  WITHDRAWN: "var(--muted)",
};

export default function PipelineSection({ data }: { data: DashboardPipeline }) {
  const statuses = Object.entries(data.submissionsByStatus);
  const max = Math.max(1, ...statuses.map(([, count]) => count));
  const trend = data.joiningsTrend.map((point) => ({ name: monthLabel(point.month), count: point.count }));

  return (
    <section aria-labelledby="pipeline-heading" className="space-y-4">
      <h2 id="pipeline-heading" className="m-0 text-lg font-semibold">
        {t("dashboard.pipeline.heading")}
      </h2>
      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3 xl:grid-cols-5">
        <StatCard
          label={t("dashboard.pipeline.openJobs")}
          value={String(data.openJobs)}
          icon={Briefcase}
          href="/jobs"
        />
        <StatCard
          label={t("dashboard.pipeline.availableCandidates")}
          value={String(data.availableCandidates)}
          hint={`${data.candidates} ${t("dashboard.pipeline.inTotal")}`}
          icon={UserCheck}
          tone="ok"
          href="/candidates"
        />
        <StatCard
          label={t("dashboard.pipeline.interviews")}
          value={String(data.interviewsNext7Days)}
          hint={t("dashboard.pipeline.next7Days")}
          icon={CalendarClock}
          tone="warn"
          href="/interviews"
        />
        <StatCard
          label={t("dashboard.pipeline.offersPending")}
          value={String(data.offersPending)}
          icon={Hourglass}
          tone="warn"
          href="/placements"
        />
        <StatCard
          label={t("dashboard.pipeline.joinings")}
          value={String(data.joiningsThisMonth)}
          hint={t("dashboard.pipeline.thisMonth")}
          icon={Handshake}
          tone="ok"
          href="/placements"
        />
      </div>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>{t("dashboard.pipeline.submissions")}</CardTitle>
            <CardDescription>{t("dashboard.pipeline.submissionsHelp")}</CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="m-0 grid list-none gap-3 p-0">
              {statuses.map(([status, count]) => (
                <li key={status}>
                  <Link
                    href="/submissions"
                    className="block text-fg no-underline"
                    aria-label={`${tx(`enums.submissionStatus.${status}`)}: ${count}`}
                  >
                    <div className="mb-1 flex items-center justify-between text-sm">
                      <span>{tx(`enums.submissionStatus.${status}`)}</span>
                      <span className="font-semibold tabular-nums">{count}</span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-bg">
                      <div
                        className="h-full rounded-full"
                        style={{ width: `${(count / max) * 100}%`, background: barColor[status] ?? "var(--accent)" }}
                      />
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>{t("dashboard.pipeline.joiningsTrend")}</CardTitle>
            <CardDescription>{t("dashboard.pipeline.last6Months")}</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-60 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={trend} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
                  <CartesianGrid vertical={false} stroke="var(--border)" />
                  <XAxis
                    dataKey="name"
                    tickLine={false}
                    axisLine={false}
                    tick={{ fill: "var(--muted)", fontSize: 12 }}
                  />
                  <YAxis
                    allowDecimals={false}
                    tickLine={false}
                    axisLine={false}
                    tick={{ fill: "var(--muted)", fontSize: 12 }}
                  />
                  <Tooltip
                    cursor={{ fill: "var(--bg)" }}
                    contentStyle={{
                      background: "var(--surface)",
                      border: "1px solid var(--border)",
                      borderRadius: 8,
                      color: "var(--text)",
                    }}
                    formatter={(value) => [String(value), t("dashboard.pipeline.joinings")]}
                  />
                  <Bar dataKey="count" fill="var(--ok)" radius={[6, 6, 0, 0]} maxBarSize={44} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>
    </section>
  );
}
