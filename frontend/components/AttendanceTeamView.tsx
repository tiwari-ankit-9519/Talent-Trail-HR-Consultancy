"use client";

import { useState } from "react";
import LeaveTeamPanel from "@/components/attendance/LeaveTeamPanel";
import RequestsPanel from "@/components/attendance/RequestsPanel";
import RulesPanel from "@/components/attendance/RulesPanel";
import SummaryPanel from "@/components/attendance/SummaryPanel";
import TeamDayPanel from "@/components/attendance/TeamDayPanel";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { t } from "@/lib/messages";

type Tab = "day" | "summary" | "requests" | "leave" | "rules";

export default function AttendanceTeamView() {
  const [tab, setTab] = useState<Tab>("day");

  return (
    <section>
      <div className="page-head">
        <h1>{t("attendance.team.heading")}</h1>
      </div>
      <Tabs value={tab} onValueChange={(value) => setTab(value as Tab)}>
        <TabsList>
          <TabsTrigger value="day">{t("attendance.team.tabs.day")}</TabsTrigger>
          <TabsTrigger value="summary">{t("attendance.team.tabs.summary")}</TabsTrigger>
          <TabsTrigger value="requests">{t("attendance.team.tabs.requests")}</TabsTrigger>
          <TabsTrigger value="leave">{t("attendance.team.tabs.leave")}</TabsTrigger>
          <TabsTrigger value="rules">{t("attendance.team.tabs.rules")}</TabsTrigger>
        </TabsList>
      </Tabs>
      {tab === "day" && <TeamDayPanel />}
      {tab === "summary" && <SummaryPanel />}
      {tab === "requests" && <RequestsPanel />}
      {tab === "leave" && <LeaveTeamPanel />}
      {tab === "rules" && <RulesPanel />}
    </section>
  );
}
