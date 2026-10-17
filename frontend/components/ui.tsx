"use client";

import { Inbox } from "lucide-react";
import type { ReactNode } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Label } from "@/components/shadcn/label";
import { Skeleton } from "@/components/shadcn/skeleton";
import { ApiError } from "@/lib/api";
import { errorText, t, tx } from "@/lib/messages";
import type { Page } from "@/lib/types";

function humanize(name: string): string {
  const spaced = name.replace(/([a-z0-9])([A-Z])/g, "$1 $2").toLowerCase();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

function fieldMessage(field: string, code: string): string {
  const base =
    field
      .replace(/\[\d+\]/g, "")
      .split(".")
      .pop() ?? field;
  const specific = tx(`validation.${base}.${code}`, "");
  if (specific) return specific;
  const label = tx(`fieldLabels.${base}`, humanize(base));
  return `${label} ${tx(`validation.${code}`, tx("validation.Invalid"))}`;
}

function bannerMessage(error: ApiError): string {
  if (error.code === "VALIDATION_FAILED" && Object.keys(error.fields).length === 0) {
    return error.message && error.message !== "Validation failed" ? error.message : errorText(error.code);
  }
  if (error.code === "UNKNOWN") {
    return tx(`errors.HTTP_${error.status}`, t("errors.UNKNOWN"));
  }
  return errorText(error.code, t("errors.UNKNOWN"));
}

export function ErrorBanner({ error }: { error: unknown }) {
  if (!error) return null;
  if (!(error instanceof ApiError)) {
    return (
      <Alert variant="destructive">
        <p>{error instanceof TypeError ? t("errors.NETWORK") : t("errors.UNKNOWN")}</p>
      </Alert>
    );
  }
  const fields = Object.entries(error.fields);
  return (
    <Alert variant="destructive">
      <p>{bannerMessage(error)}</p>
      {fields.length > 0 && (
        <ul>
          {fields.map(([name, code]) => (
            <li key={name}>{fieldMessage(name, code)}</li>
          ))}
        </ul>
      )}
    </Alert>
  );
}

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <Label className="field grid items-stretch gap-1.5 font-normal leading-normal">
      <span className="field-label">{label}</span>
      {children}
      {hint && <span className="field-hint">{hint}</span>}
    </Label>
  );
}

export function StatusBadge({ status }: { status: "ACTIVE" | "INACTIVE" }) {
  return (
    <Badge variant={status === "ACTIVE" ? "ok" : "default"}>
      {t(status === "ACTIVE" ? "common.active" : "common.inactive")}
    </Badge>
  );
}

function pageWindow(current: number, total: number): (number | "gap")[] {
  const pages = new Set<number>([0, total - 1, current - 1, current, current + 1]);
  const sorted = [...pages].filter((value) => value >= 0 && value < total).sort((a, b) => a - b);
  const result: (number | "gap")[] = [];
  sorted.forEach((value, index) => {
    if (index > 0 && value - sorted[index - 1] > 1) result.push("gap");
    result.push(value);
  });
  return result;
}

export function Pager<T>({ page, onChange }: { page: Page<T> | null; onChange: (next: number) => void }) {
  if (!page || page.totalItems === 0) return null;
  const from = page.page * page.size + 1;
  const to = Math.min(page.totalItems, (page.page + 1) * page.size);
  const summary = t("common.showing")
    .replace("{from}", String(from))
    .replace("{to}", String(to))
    .replace("{total}", String(page.totalItems));
  return (
    <div className="pager">
      <span className="text-sm text-muted">{summary}</span>
      {page.totalPages > 1 && (
        <div className="flex flex-wrap items-center gap-1">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={page.page <= 0}
            onClick={() => onChange(page.page - 1)}
          >
            {t("common.previous")}
          </Button>
          {pageWindow(page.page, page.totalPages).map((entry, index) =>
            entry === "gap" ? (
              <span key={`gap-${index}`} aria-hidden="true" className="px-1 text-muted">
                …
              </span>
            ) : (
              <Button
                key={entry}
                type="button"
                size="sm"
                variant={entry === page.page ? "default" : "outline"}
                aria-current={entry === page.page ? "page" : undefined}
                aria-label={`${t("common.page")} ${entry + 1}`}
                className="min-w-8 px-2"
                onClick={() => onChange(entry)}
              >
                {entry + 1}
              </Button>
            ),
          )}
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={page.page + 1 >= page.totalPages}
            onClick={() => onChange(page.page + 1)}
          >
            {t("common.next")}
          </Button>
        </div>
      )}
    </div>
  );
}

export function Loading() {
  return (
    <div className="grid gap-2" role="status" aria-live="polite">
      <span className="sr-only">{t("common.loading")}</span>
      <Skeleton className="h-9 w-full" />
      <Skeleton className="h-9 w-full" />
      <Skeleton className="h-9 w-2/3" />
    </div>
  );
}

export function Empty() {
  return (
    <div className="grid place-items-center gap-2 rounded-card border border-dashed border-line bg-surface px-4 py-10 text-center">
      <Inbox aria-hidden="true" className="size-8 text-muted" />
      <p className="m-0 text-sm text-muted">{t("common.empty")}</p>
    </div>
  );
}
