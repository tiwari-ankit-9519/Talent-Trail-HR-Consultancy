"use client";

import { useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import FunnelPanel from "@/components/reports/FunnelPanel";
import JoiningsPanel from "@/components/reports/JoiningsPanel";
import RecruitersPanel from "@/components/reports/RecruitersPanel";
import ReplacementsPanel from "@/components/reports/ReplacementsPanel";
import ReportFiltersBar, { presetRange } from "@/components/reports/ReportFilters";
import RevenuePanel from "@/components/reports/RevenuePanel";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { t } from "@/lib/messages";
import type { ReportFilters } from "@/lib/useReport";

type Tab = "joinings" | "revenue" | "recruiters" | "funnel" | "replacements";

export default function ReportsView() {
  const { can } = useAuth();
  const canSeeRevenue = can("INVOICES_VIEW") || can("INVOICES_MANAGE");
  const [tab, setTab] = useState<Tab>("joinings");
  const [filters, setFilters] = useState<ReportFilters>({ ...presetRange("half"), clientId: "" });

  return (
    <section>
      <div className="page-head">
        <h1>{t("reports.heading")}</h1>
      </div>
      <ReportFiltersBar filters={filters} onChange={setFilters} />
      <Tabs value={tab} onValueChange={(value) => setTab(value as Tab)}>
        <TabsList>
          <TabsTrigger value="joinings">{t("reports.tabs.joinings")}</TabsTrigger>
          {canSeeRevenue && <TabsTrigger value="revenue">{t("reports.tabs.revenue")}</TabsTrigger>}
          <TabsTrigger value="recruiters">{t("reports.tabs.recruiters")}</TabsTrigger>
          <TabsTrigger value="funnel">{t("reports.tabs.funnel")}</TabsTrigger>
          <TabsTrigger value="replacements">{t("reports.tabs.replacements")}</TabsTrigger>
        </TabsList>
      </Tabs>
      {tab === "joinings" && <JoiningsPanel filters={filters} />}
      {tab === "revenue" && canSeeRevenue && <RevenuePanel filters={filters} />}
      {tab === "recruiters" && <RecruitersPanel filters={filters} />}
      {tab === "funnel" && <FunnelPanel filters={filters} />}
      {tab === "replacements" && <ReplacementsPanel filters={filters} />}
    </section>
  );
}
