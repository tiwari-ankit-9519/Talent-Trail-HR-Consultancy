"use client";

import { useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import AttendanceReportPanel from "@/components/reports/AttendanceReportPanel";
import CommissionsReportPanel from "@/components/reports/CommissionsReportPanel";
import FunnelPanel from "@/components/reports/FunnelPanel";
import JoiningsPanel from "@/components/reports/JoiningsPanel";
import LeaveReportPanel from "@/components/reports/LeaveReportPanel";
import PayrollRegisterPanel from "@/components/reports/PayrollRegisterPanel";
import ReceivablesPanel from "@/components/reports/ReceivablesPanel";
import RecruitersPanel from "@/components/reports/RecruitersPanel";
import ReplacementsPanel from "@/components/reports/ReplacementsPanel";
import ReportFiltersBar, { presetRange } from "@/components/reports/ReportFilters";
import RevenuePanel from "@/components/reports/RevenuePanel";
import TaxPanel from "@/components/reports/TaxPanel";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { t } from "@/lib/messages";
import type { ReportFilters } from "@/lib/useReport";

type Tab =
  | "joinings"
  | "revenue"
  | "recruiters"
  | "funnel"
  | "replacements"
  | "payroll"
  | "attendance"
  | "leave"
  | "receivables"
  | "tax"
  | "commissions";

const filterless: Tab[] = ["payroll", "attendance", "leave"];

export default function ReportsView() {
  const { can } = useAuth();
  const canReports = can("REPORTS_VIEW");
  const canInvoices = can("INVOICES_VIEW") || can("INVOICES_MANAGE");
  const canRevenue = canReports && canInvoices;
  const canPayroll = can("PAYROLL_MANAGE");
  const canAttendance = can("ATTENDANCE_MANAGE");
  const canCommissions = can("COMMISSIONS_VIEW") || can("COMMISSIONS_MANAGE");
  const first: Tab = canReports
    ? "joinings"
    : canInvoices
      ? "receivables"
      : canCommissions
        ? "commissions"
        : canPayroll
          ? "payroll"
          : "attendance";
  const [tab, setTab] = useState<Tab>(first);
  const [filters, setFilters] = useState<ReportFilters>({ ...presetRange("half"), clientId: "" });

  return (
    <section>
      <div className="page-head">
        <h1>{t("reports.heading")}</h1>
      </div>
      {!filterless.includes(tab) && <ReportFiltersBar filters={filters} onChange={setFilters} />}
      <Tabs value={tab} onValueChange={(value) => setTab(value as Tab)}>
        <TabsList>
          {canReports && <TabsTrigger value="joinings">{t("reports.tabs.joinings")}</TabsTrigger>}
          {canRevenue && <TabsTrigger value="revenue">{t("reports.tabs.revenue")}</TabsTrigger>}
          {canReports && <TabsTrigger value="recruiters">{t("reports.tabs.recruiters")}</TabsTrigger>}
          {canReports && <TabsTrigger value="funnel">{t("reports.tabs.funnel")}</TabsTrigger>}
          {canReports && <TabsTrigger value="replacements">{t("reports.tabs.replacements")}</TabsTrigger>}
          {canInvoices && <TabsTrigger value="receivables">{t("reports.tabs.receivables")}</TabsTrigger>}
          {canInvoices && <TabsTrigger value="tax">{t("reports.tabs.tax")}</TabsTrigger>}
          {canCommissions && <TabsTrigger value="commissions">{t("reports.tabs.commissions")}</TabsTrigger>}
          {canPayroll && <TabsTrigger value="payroll">{t("reports.tabs.payroll")}</TabsTrigger>}
          {canAttendance && <TabsTrigger value="attendance">{t("reports.tabs.attendance")}</TabsTrigger>}
          {canAttendance && <TabsTrigger value="leave">{t("reports.tabs.leave")}</TabsTrigger>}
        </TabsList>
      </Tabs>
      {tab === "joinings" && canReports && <JoiningsPanel filters={filters} />}
      {tab === "revenue" && canRevenue && <RevenuePanel filters={filters} />}
      {tab === "recruiters" && canReports && <RecruitersPanel filters={filters} />}
      {tab === "funnel" && canReports && <FunnelPanel filters={filters} />}
      {tab === "replacements" && canReports && <ReplacementsPanel filters={filters} />}
      {tab === "receivables" && canInvoices && <ReceivablesPanel filters={filters} />}
      {tab === "tax" && canInvoices && <TaxPanel filters={filters} />}
      {tab === "commissions" && canCommissions && <CommissionsReportPanel filters={filters} />}
      {tab === "payroll" && canPayroll && <PayrollRegisterPanel />}
      {tab === "attendance" && canAttendance && <AttendanceReportPanel />}
      {tab === "leave" && canAttendance && <LeaveReportPanel />}
    </section>
  );
}
