"use client";

import { useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import ImportPanel from "@/components/import/ImportPanel";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import type { ImportType } from "@/lib/importConfig";
import { t } from "@/lib/messages";

export default function ImportView() {
  const { can } = useAuth();
  const allowed: ImportType[] = [];
  if (can("CANDIDATES_MANAGE")) allowed.push("candidates");
  if (can("CLIENTS_MANAGE")) allowed.push("clients");
  if (can("USERS_MANAGE") && can("PROFILES_MANAGE")) allowed.push("employees");
  const [type, setType] = useState<ImportType | null>(null);
  const active = type && allowed.includes(type) ? type : (allowed[0] ?? null);

  return (
    <section>
      <div className="page-head">
        <h1>{t("import.heading")}</h1>
      </div>
      {active && (
        <>
          <Tabs value={active} onValueChange={(value) => setType(value as ImportType)}>
            <TabsList>
              {allowed.includes("candidates") && (
                <TabsTrigger value="candidates">{t("import.tabs.candidates")}</TabsTrigger>
              )}
              {allowed.includes("clients") && <TabsTrigger value="clients">{t("import.tabs.clients")}</TabsTrigger>}
              {allowed.includes("employees") && (
                <TabsTrigger value="employees">{t("import.tabs.employees")}</TabsTrigger>
              )}
            </TabsList>
          </Tabs>
          <ImportPanel key={active} type={active} />
        </>
      )}
    </section>
  );
}
