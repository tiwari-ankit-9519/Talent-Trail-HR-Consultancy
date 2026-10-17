"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";

export type ReportFilters = { from: string; to: string; clientId: string };

export function useReport<T>(path: string, filters: ReportFilters) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api<T>(path, {
      query: { from: filters.from || undefined, to: filters.to || undefined, clientId: filters.clientId || undefined },
    })
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((failure) => {
        if (!cancelled) {
          setData(null);
          setError(failure);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [path, filters.from, filters.to, filters.clientId]);

  return { data, error, loading };
}
