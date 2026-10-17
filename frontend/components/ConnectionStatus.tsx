"use client";

import { Button } from "@/components/shadcn/button";
import { useCallback, useEffect, useState } from "react";
import { t } from "@/lib/messages";

type State = "checking" | "up" | "down";

const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api";

export default function ConnectionStatus() {
  const [state, setState] = useState<State>("checking");

  const check = useCallback(async () => {
    setState("checking");
    try {
      const response = await fetch(`${apiBase}/v1/system/status`, {
        cache: "no-store",
      });
      setState(response.ok ? "up" : "down");
    } catch {
      setState("down");
    }
  }, []);

  useEffect(() => {
    void check();
  }, [check]);

  return (
    <section className="status-card" aria-live="polite">
      <span className="status-label">{t("connectionStatus.label")}</span>
      <span className={`status-pill status-${state}`}>{t(`connectionStatus.${state}`)}</span>
      <Button type="button" variant="outline" size="sm" onClick={() => void check()}>
        {t("connectionStatus.retry")}
      </Button>
    </section>
  );
}
