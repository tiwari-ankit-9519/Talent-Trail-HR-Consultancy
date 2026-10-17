"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Empty, ErrorBanner, Loading, Pager } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDay, formatMoney, t, tx } from "@/lib/messages";
import type { Page, PlacementStatus, PlacementSummary } from "@/lib/types";
import { FormSelect } from "@/components/shadcn/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";

const statuses: PlacementStatus[] = ["OFFERED", "OFFER_ACCEPTED", "JOINED", "OFFER_DECLINED", "NOT_JOINED"];

export default function PlacementsView() {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<Page<PlacementSummary> | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setResult(
        await api<Page<PlacementSummary>>("/placements", { query: { status: status || undefined, page, size: 20 } }),
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
      <h1>{t("placements.pageHeading")}</h1>
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
              {tx(`enums.placementStatus.${value}`)}
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
                <TableHead>{t("placements.offeredCtc")}</TableHead>
                <TableHead>{t("placements.expectedJoiningDate")}</TableHead>
                <TableHead>{t("placements.joiningDate")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("placements.joiningEmail")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {result.items.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>
                    <Link href={`/placements/${row.id}`}>{row.candidateName}</Link>
                  </TableCell>
                  <TableCell>
                    {row.jobCode} · {row.jobTitle}
                  </TableCell>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>{formatMoney(row.offeredCtc)}</TableCell>
                  <TableCell>{formatDay(row.expectedJoiningDate)}</TableCell>
                  <TableCell>{formatDay(row.joiningDate)}</TableCell>
                  <TableCell>
                    <StatusPill kind="placementStatus" value={row.status} />
                  </TableCell>
                  <TableCell>
                    {row.status === "JOINED"
                      ? row.joiningEmailAttached
                        ? t("placements.proofAttached")
                        : t("placements.proofMissing")
                      : ""}
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
