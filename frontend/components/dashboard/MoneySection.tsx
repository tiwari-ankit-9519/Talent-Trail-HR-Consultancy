"use client";

import Link from "next/link";
import { AlertTriangle, ArrowRight, FilePlus2, IndianRupee, Mail, ReceiptText, Wallet } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import StatCard from "@/components/dashboard/StatCard";
import { compactMoney, fullMoney, monthLabel } from "@/components/dashboard/format";
import { Badge } from "@/components/shadcn/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/shadcn/card";
import { t } from "@/lib/messages";
import type { DashboardMoney } from "@/lib/types";

export default function MoneySection({ data }: { data: DashboardMoney }) {
  const trend = data.trend.map((point) => ({
    name: monthLabel(point.month),
    invoiced: point.invoiced,
    collected: point.collected,
  }));
  const actions = [
    {
      key: "ready",
      count: data.readyToInvoice,
      label: t("dashboard.money.readyToInvoice"),
      hint: t("dashboard.money.readyToInvoiceHint"),
      href: "/placements",
      icon: FilePlus2,
      variant: "accent" as const,
    },
    {
      key: "awaiting",
      count: data.awaitingJoiningEmail,
      label: t("dashboard.money.awaitingEmail"),
      hint: t("dashboard.money.awaitingEmailHint"),
      href: "/placements",
      icon: Mail,
      variant: "warn" as const,
    },
    {
      key: "overdue",
      count: data.overdueCount,
      label: t("dashboard.money.overdueInvoices"),
      hint: `${fullMoney(data.overdueAmount)} ${t("dashboard.money.stillDue")}`,
      href: "/invoices",
      icon: AlertTriangle,
      variant: "bad" as const,
    },
  ];

  return (
    <section aria-labelledby="money-heading" className="space-y-4">
      <h2 id="money-heading" className="m-0 text-lg font-semibold">
        {t("dashboard.money.heading")}
      </h2>
      <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
        <StatCard
          label={t("dashboard.money.invoiced")}
          value={compactMoney(data.invoicedThisMonth)}
          title={fullMoney(data.invoicedThisMonth)}
          hint={t("dashboard.pipeline.thisMonth")}
          icon={ReceiptText}
        />
        <StatCard
          label={t("dashboard.money.collected")}
          value={compactMoney(data.collectedThisMonth)}
          title={fullMoney(data.collectedThisMonth)}
          hint={t("dashboard.pipeline.thisMonth")}
          icon={Wallet}
          tone="ok"
        />
        <StatCard
          label={t("dashboard.money.outstanding")}
          value={compactMoney(data.outstanding)}
          title={fullMoney(data.outstanding)}
          hint={t("dashboard.money.allOpen")}
          icon={IndianRupee}
          tone="warn"
          href="/invoices"
        />
        <StatCard
          label={t("dashboard.money.overdue")}
          value={compactMoney(data.overdueAmount)}
          title={fullMoney(data.overdueAmount)}
          hint={`${data.overdueCount} ${t("dashboard.money.invoicesCount")}`}
          icon={AlertTriangle}
          tone="bad"
          href="/invoices"
        />
      </div>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>{t("dashboard.money.trend")}</CardTitle>
            <CardDescription>{t("dashboard.pipeline.last6Months")}</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={trend} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                  <CartesianGrid vertical={false} stroke="var(--border)" />
                  <XAxis
                    dataKey="name"
                    tickLine={false}
                    axisLine={false}
                    tick={{ fill: "var(--muted)", fontSize: 12 }}
                  />
                  <YAxis
                    tickLine={false}
                    axisLine={false}
                    tick={{ fill: "var(--muted)", fontSize: 12 }}
                    tickFormatter={(value: number) => compactMoney(value).replace("₹ ", "")}
                  />
                  <Tooltip
                    cursor={{ fill: "var(--bg)" }}
                    contentStyle={{
                      background: "var(--surface)",
                      border: "1px solid var(--border)",
                      borderRadius: 8,
                      color: "var(--text)",
                    }}
                    formatter={(value) => fullMoney(Number(value))}
                  />
                  <Legend wrapperStyle={{ fontSize: 12, color: "var(--muted)" }} />
                  <Bar
                    dataKey="invoiced"
                    name={t("dashboard.money.invoiced")}
                    fill="var(--accent)"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={32}
                  />
                  <Bar
                    dataKey="collected"
                    name={t("dashboard.money.collected")}
                    fill="var(--ok)"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={32}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>{t("dashboard.money.needsAttention")}</CardTitle>
            <CardDescription>{t("dashboard.money.needsAttentionHelp")}</CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="m-0 grid list-none gap-2 p-0">
              {actions.map((action) => {
                const Icon = action.icon;
                return (
                  <li key={action.key}>
                    <Link
                      href={action.href}
                      className="flex items-center gap-3 rounded-xl border border-line p-3 text-fg no-underline transition-colors hover:bg-bg"
                    >
                      <Icon aria-hidden="true" className="size-5 shrink-0 text-muted" />
                      <span className="min-w-0 flex-1 leading-tight">
                        <span className="block truncate text-sm font-medium">{action.label}</span>
                        <span className="block truncate text-xs text-muted">{action.hint}</span>
                      </span>
                      <Badge variant={action.count > 0 ? action.variant : "default"}>{action.count}</Badge>
                      <ArrowRight aria-hidden="true" className="size-4 shrink-0 text-muted" />
                    </Link>
                  </li>
                );
              })}
            </ul>
          </CardContent>
        </Card>
      </div>
    </section>
  );
}
