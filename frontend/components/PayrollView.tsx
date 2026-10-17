"use client";

import { useState } from "react";
import PayrollRulesPanel from "@/components/payroll/PayrollRulesPanel";
import PayrollRunPanel from "@/components/payroll/PayrollRunPanel";
import StructuresPanel from "@/components/payroll/StructuresPanel";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { t } from "@/lib/messages";

type Tab = "runs" | "structures" | "rules";

export default function PayrollView() {
  const [tab, setTab] = useState<Tab>("runs");
  return (
    <section>
      <div className="page-head">
        <h1>{t("payroll.heading")}</h1>
      </div>
      <Tabs value={tab} onValueChange={(value) => setTab(value as Tab)}>
        <TabsList>
          <TabsTrigger value="runs">{t("payroll.tabs.runs")}</TabsTrigger>
          <TabsTrigger value="structures">{t("payroll.tabs.structures")}</TabsTrigger>
          <TabsTrigger value="rules">{t("payroll.tabs.rules")}</TabsTrigger>
        </TabsList>
      </Tabs>
      <div className="mt-4">
        {tab === "runs" && <PayrollRunPanel />}
        {tab === "structures" && <StructuresPanel />}
        {tab === "rules" && <PayrollRulesPanel />}
      </div>
    </section>
  );
}
