"use client";

import { useState } from "react";
import RemindersPanel from "@/components/emails/RemindersPanel";
import ReminderHistoryPanel from "@/components/emails/ReminderHistoryPanel";
import TemplatesPanel from "@/components/emails/TemplatesPanel";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { t } from "@/lib/messages";

type Tab = "templates" | "reminders" | "history";

export default function EmailSettingsView() {
  const [tab, setTab] = useState<Tab>("templates");
  return (
    <section>
      <div className="page-head">
        <h1>{t("emails.heading")}</h1>
      </div>
      <Tabs value={tab} onValueChange={(value) => setTab(value as Tab)}>
        <TabsList>
          <TabsTrigger value="templates">{t("emails.tabs.templates")}</TabsTrigger>
          <TabsTrigger value="reminders">{t("emails.tabs.reminders")}</TabsTrigger>
          <TabsTrigger value="history">{t("emails.tabs.history")}</TabsTrigger>
        </TabsList>
      </Tabs>
      {tab === "templates" && <TemplatesPanel />}
      {tab === "reminders" && <RemindersPanel />}
      {tab === "history" && <ReminderHistoryPanel />}
    </section>
  );
}
