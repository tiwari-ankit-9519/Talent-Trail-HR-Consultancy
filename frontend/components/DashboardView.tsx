"use client";

import { useEffect, useState } from "react";
import ConnectionStatus from "@/components/ConnectionStatus";
import { useAuth } from "@/components/AuthProvider";
import MoneySection from "@/components/dashboard/MoneySection";
import PipelineSection from "@/components/dashboard/PipelineSection";
import { Badge } from "@/components/shadcn/badge";
import { Card } from "@/components/shadcn/card";
import { Skeleton } from "@/components/shadcn/skeleton";
import { ErrorBanner } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { DashboardData } from "@/lib/types";

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return t("dashboard.greeting.morning");
  if (hour < 17) return t("dashboard.greeting.afternoon");
  return t("dashboard.greeting.evening");
}

function LoadingState() {
  return (
    <div className="space-y-8" aria-busy="true" aria-live="polite">
      <span className="sr-only">{t("common.loading")}</span>
      <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => (
          <Card key={index} className="p-4">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="mt-3 h-8 w-32" />
            <Skeleton className="mt-3 h-3 w-20" />
          </Card>
        ))}
      </div>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Skeleton className="h-72 rounded-card" />
        <Skeleton className="h-72 rounded-card" />
      </div>
    </div>
  );
}

export default function DashboardView() {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    void (async () => {
      try {
        setError(null);
        setData(await api<DashboardData>("/dashboard"));
      } catch (failure) {
        setError(failure);
      }
    })();
  }, []);

  const firstName = user?.fullName.split(/\s+/)[0] ?? "";
  const today = new Date().toLocaleDateString(undefined, {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="m-0 text-2xl font-semibold tracking-tight sm:text-3xl">
            {greeting()}, {firstName}
          </h1>
          <p className="m-0 mt-1 text-sm text-muted">{today}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2" aria-label={t("dashboard.roles")}>
          {user?.roles.map((role) => (
            <Badge key={role} variant="accent">
              {role}
            </Badge>
          ))}
        </div>
      </header>

      <ErrorBanner error={error} />
      {!data && !error && <LoadingState />}
      {data && !data.pipeline && !data.money && (
        <Card className="p-6">
          <p className="m-0 text-muted">{t("dashboard.noData")}</p>
        </Card>
      )}
      {data?.pipeline && <PipelineSection data={data.pipeline} />}
      {data?.money && <MoneySection data={data.money} />}

      <footer className="pt-2">
        <ConnectionStatus />
      </footer>
    </div>
  );
}
