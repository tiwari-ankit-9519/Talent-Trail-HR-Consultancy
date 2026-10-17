"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Empty, ErrorBanner, Loading, Pager } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDay, t, tx } from "@/lib/messages";
import type { Page, ReplacementStatus, ReplacementView } from "@/lib/types";
import { FormSelect } from "@/components/shadcn/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";

const statuses: ReplacementStatus[] = ["OPEN", "FILLED", "CLOSED", "NOT_ELIGIBLE"];

export default function ReplacementsView() {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<Page<ReplacementView> | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setResult(
        await api<Page<ReplacementView>>("/replacements", { query: { status: status || undefined, page, size: 20 } }),
      );
    } catch (failure) {
      setError(failure);
    }
  }, [status, page]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section>
      <h1>{t("replacements.pageHeading")}</h1>
      <div className="toolbar">
        <FormSelect
          value={status}
          aria-label={t("common.status")}
          onChange={(e) => {
            setPage(0);
            setStatus(e.target.value);
          }}
        >
          <option value="">{t("common.allStatuses")}</option>
          {statuses.map((value) => (
            <option key={value} value={value}>
              {tx(`enums.replacementStatus.${value}`)}
            </option>
          ))}
        </FormSelect>
      </div>
      <ErrorBanner error={error} />
      {!result && !error && <Loading />}
      {result && result.items.length === 0 && <Empty />}
      {result && result.items.length > 0 && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("candidates.fullName")}</TableHead>
                <TableHead>{t("jobs.title")}</TableHead>
                <TableHead>{t("jobs.client")}</TableHead>
                <TableHead>{t("placements.joiningDate")}</TableHead>
                <TableHead>{t("replacements.exitDate")}</TableHead>
                <TableHead>{t("replacements.freeUntil")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("replacements.replacedBy")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {result.items.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>
                    <Link href={`/placements/${row.placementId}`}>{row.candidateName}</Link>
                  </TableCell>
                  <TableCell>
                    {row.jobCode} · {row.jobTitle}
                  </TableCell>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>{formatDay(row.joiningDate)}</TableCell>
                  <TableCell>{formatDay(row.exitDate)}</TableCell>
                  <TableCell>{formatDay(row.freeUntil)}</TableCell>
                  <TableCell>
                    <StatusPill kind="replacementStatus" value={row.status} />
                  </TableCell>
                  <TableCell>
                    {row.replacementPlacementId && (
                      <Link href={`/placements/${row.replacementPlacementId}`}>{row.replacementCandidateName}</Link>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      <Pager page={result} onChange={setPage} />
    </section>
  );
}
