"use client";

import { CheckCircle2, RefreshCw, XCircle } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { t, tx } from "@/lib/messages";
import type { SystemHealth } from "@/lib/types";

export default function SystemHealthView() {
  const [report, setReport] = useState<SystemHealth | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setBusy(true);
    try {
      setError(null);
      setReport(await api<SystemHealth>("/system/health"));
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }, []);

  useEffect(() => {
    void load();
    const timer = setInterval(() => void load(), 30000);
    return () => clearInterval(timer);
  }, [load]);

  return (
    <section>
      <div className="page-head flex flex-wrap items-center justify-between gap-3">
        <h1>{t("system.heading")}</h1>
        <Button type="button" variant="outline" disabled={busy} onClick={() => void load()}>
          <RefreshCw aria-hidden="true" className="size-4" />
          {t("system.refresh")}
        </Button>
      </div>
      <p className="muted m-0 mb-4 max-w-3xl text-sm">{t("system.help")}</p>
      <ErrorBanner error={error} />
      {!report && !error && <Loading />}
      {report && (
        <div className="grid gap-4">
          <div className="grid gap-2 sm:grid-cols-3">
            <Card className="px-4 py-3">
              <p className="m-0 text-xs text-muted">{t("system.overall")}</p>
              <p className="m-0 mt-1">
                <Badge variant={report.healthy ? "ok" : "warn"}>
                  {report.healthy ? t("system.healthy") : t("system.attention")}
                </Badge>
              </p>
            </Card>
            <Card className="px-4 py-3">
              <p className="m-0 text-xs text-muted">{t("system.started")}</p>
              <p className="m-0 text-sm font-semibold">{new Date(report.startedAt).toLocaleString()}</p>
            </Card>
            <Card className="px-4 py-3">
              <p className="m-0 text-xs text-muted">{t("system.liveConnections")}</p>
              <p className="m-0 text-lg font-semibold">{report.liveConnections}</p>
            </Card>
          </div>
          <Card className="p-0">
            <ul className="m-0 grid list-none p-0">
              {report.checks.map((check) => (
                <li key={check.key} className="flex items-start gap-3 border-b border-line px-5 py-4 last:border-b-0">
                  {check.ok ? (
                    <CheckCircle2 aria-hidden="true" className="mt-0.5 size-5 shrink-0 text-ok" />
                  ) : (
                    <XCircle aria-hidden="true" className="mt-0.5 size-5 shrink-0 text-bad" />
                  )}
                  <div className="min-w-0">
                    <p className="m-0 text-sm font-semibold">{tx(`system.checks.${check.key}`)}</p>
                    <p className="muted m-0 text-sm">{check.detail}</p>
                  </div>
                </li>
              ))}
            </ul>
          </Card>
          <p className="muted m-0 text-xs">
            {t("system.checkedAt").replace("{time}", new Date(report.checkedAt).toLocaleTimeString())}
          </p>
        </div>
      )}
    </section>
  );
}
