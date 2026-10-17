"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";

export function useFetch<T>(path: string, query: Record<string, string | number | undefined>) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const key = JSON.stringify(query);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api<T>(path, { query: JSON.parse(key) as Record<string, string | number | undefined> })
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
  }, [path, key]);

  return { data, error, loading };
}
