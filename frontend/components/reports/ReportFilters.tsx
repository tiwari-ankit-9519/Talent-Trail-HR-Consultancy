"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Field } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { ClientSummary, Page } from "@/lib/types";
import type { ReportFilters } from "@/lib/useReport";
import { DateField } from "@/components/shadcn/date-picker";

function iso(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

export function presetRange(key: "month" | "quarter" | "half" | "year" | "fy"): { from: string; to: string } {
  const today = new Date();
  const to = iso(today);
  if (key === "month") return { from: iso(new Date(today.getFullYear(), today.getMonth(), 1)), to };
  if (key === "quarter") return { from: iso(new Date(today.getFullYear(), today.getMonth() - 2, 1)), to };
  if (key === "half") return { from: iso(new Date(today.getFullYear(), today.getMonth() - 5, 1)), to };
  if (key === "year") return { from: iso(new Date(today.getFullYear(), today.getMonth() - 11, 1)), to };
  const startYear = today.getMonth() >= 3 ? today.getFullYear() : today.getFullYear() - 1;
  return { from: iso(new Date(startYear, 3, 1)), to };
}

const presets: { key: "month" | "quarter" | "half" | "year" | "fy"; label: string }[] = [
  { key: "month", label: t("reports.presets.month") },
  { key: "quarter", label: t("reports.presets.quarter") },
  { key: "half", label: t("reports.presets.half") },
  { key: "fy", label: t("reports.presets.fy") },
  { key: "year", label: t("reports.presets.year") },
];

export default function ReportFiltersBar({
  filters,
  onChange,
}: {
  filters: ReportFilters;
  onChange: (next: ReportFilters) => void;
}) {
  const [clients, setClients] = useState<ClientSummary[]>([]);

  useEffect(() => {
    api<Page<ClientSummary>>("/clients", { query: { size: 100 } })
      .then((page) => setClients(page.items))
      .catch(() => setClients([]));
  }, []);

  return (
    <Card className="mb-5 grid gap-4 p-4">
      <div className="form-grid">
        <Field label={t("reports.from")}>
          <DateField
            value={filters.from}
            max={filters.to || undefined}
            onChange={(e) => onChange({ ...filters, from: e.target.value })}
          />
        </Field>
        <Field label={t("reports.to")}>
          <DateField
            value={filters.to}
            min={filters.from || undefined}
            onChange={(e) => onChange({ ...filters, to: e.target.value })}
          />
        </Field>
        <Field label={t("reports.client")}>
          <FormSelect value={filters.clientId} onChange={(e) => onChange({ ...filters, clientId: e.target.value })}>
            <option value="">{t("reports.allClients")}</option>
            {clients.map((client) => (
              <option key={client.id} value={client.id}>
                {client.name}
              </option>
            ))}
          </FormSelect>
        </Field>
      </div>
      <div className="flex flex-wrap gap-2">
        {presets.map((preset) => (
          <Button
            key={preset.key}
            type="button"
            size="sm"
            variant="outline"
            onClick={() => onChange({ ...filters, ...presetRange(preset.key) })}
          >
            {preset.label}
          </Button>
        ))}
      </div>
    </Card>
  );
}
