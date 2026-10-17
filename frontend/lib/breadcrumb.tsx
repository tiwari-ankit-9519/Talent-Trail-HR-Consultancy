"use client";

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

type Context = { label: string | null; setLabel: (value: string | null) => void };

const BreadcrumbContext = createContext<Context>({ label: null, setLabel: () => {} });

export function BreadcrumbProvider({ children }: { children: ReactNode }) {
  const [label, setLabel] = useState<string | null>(null);
  const value = useMemo(() => ({ label, setLabel }), [label]);
  return <BreadcrumbContext.Provider value={value}>{children}</BreadcrumbContext.Provider>;
}

export function useBreadcrumbLabelValue(): string | null {
  return useContext(BreadcrumbContext).label;
}

export function useBreadcrumbLabel(label: string | null | undefined) {
  const { setLabel } = useContext(BreadcrumbContext);
  useEffect(() => {
    setLabel(label ?? null);
    return () => setLabel(null);
  }, [label, setLabel]);
}
