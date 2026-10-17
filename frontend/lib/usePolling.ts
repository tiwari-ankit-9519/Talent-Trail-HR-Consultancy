"use client";

import { useEffect, useRef } from "react";

export function usePolling(active: boolean, refresh: () => void | Promise<void>, intervalMs = 2500) {
  const latest = useRef(refresh);
  useEffect(() => {
    latest.current = refresh;
  }, [refresh]);

  useEffect(() => {
    if (!active) return;
    const timer = window.setInterval(() => void latest.current(), intervalMs);
    return () => window.clearInterval(timer);
  }, [active, intervalMs]);
}
