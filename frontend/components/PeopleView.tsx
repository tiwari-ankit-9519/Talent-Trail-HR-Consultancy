"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Badge } from "@/components/shadcn/badge";
import { Input } from "@/components/shadcn/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Empty, ErrorBanner, Loading, StatusBadge } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { ProfileSummary } from "@/lib/types";

function formatDate(value: string | null): string {
  if (!value) return "—";
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

export default function PeopleView() {
  const [rows, setRows] = useState<ProfileSummary[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [query, setQuery] = useState("");
  const [applied, setApplied] = useState("");

  useEffect(() => {
    const timer = setTimeout(() => setApplied(query.trim()), 250);
    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    api<ProfileSummary[]>(`/profiles${applied ? `?q=${encodeURIComponent(applied)}` : ""}`)
      .then((value) => {
        setRows(value);
        setError(null);
      })
      .catch(setError);
  }, [applied]);

  return (
    <section>
      <div className="page-head">
        <h1>{t("profile.heading")}</h1>
      </div>
      <div className="toolbar">
        <Input
          type="search"
          aria-label={t("profile.search")}
          placeholder={t("profile.search")}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>
      <ErrorBanner error={error} />
      {!rows && !error && <Loading />}
      {rows && rows.length === 0 && <Empty />}
      {rows && rows.length > 0 && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("profile.columns.name")}</TableHead>
                <TableHead>{t("profile.columns.employeeCode")}</TableHead>
                <TableHead>{t("profile.columns.designation")}</TableHead>
                <TableHead>{t("profile.columns.joined")}</TableHead>
                <TableHead>{t("profile.columns.manager")}</TableHead>
                <TableHead>{t("profile.columns.bank")}</TableHead>
                <TableHead>{t("profile.columns.documents")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.userId}>
                  <TableCell>
                    <Link href={`/people/${row.userId}`} className="font-medium">
                      {row.fullName}
                    </Link>
                    <div className="muted small">{row.email}</div>
                  </TableCell>
                  <TableCell>{row.employeeCode ?? "—"}</TableCell>
                  <TableCell>
                    {row.designation ?? "—"}
                    {row.department && <div className="muted small">{row.department}</div>}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">{formatDate(row.joiningDate)}</TableCell>
                  <TableCell>{row.managerName ?? "—"}</TableCell>
                  <TableCell>
                    <Badge variant={row.bankComplete ? "ok" : "warn"}>
                      {row.bankComplete ? t("profile.bankComplete") : t("profile.bankMissing")}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {row.documentCount}
                    {row.expiringDocuments > 0 && (
                      <>
                        {" "}
                        <Badge variant="warn">
                          {t("profile.expiringCount").replace("{count}", String(row.expiringDocuments))}
                        </Badge>
                      </>
                    )}
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={row.active ? "ACTIVE" : "INACTIVE"} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  );
}
