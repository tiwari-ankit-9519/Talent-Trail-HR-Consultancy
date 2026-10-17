"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Empty, ErrorBanner, Loading, Pager } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t, tx } from "@/lib/messages";
import type { InterviewListItem, InterviewStatus, Page } from "@/lib/types";
import { FormSelect } from "@/components/shadcn/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";

const statuses: InterviewStatus[] = ["SCHEDULED", "COMPLETED", "CANCELLED", "NO_SHOW"];

export default function InterviewsView() {
  const [status, setStatus] = useState<string>("SCHEDULED");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<Page<InterviewListItem> | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setResult(
        await api<Page<InterviewListItem>>("/interviews", {
          query: { status: status || undefined, page, size: 20 },
        }),
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
      <h1>{t("interviews.pageHeading")}</h1>
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
              {tx(`enums.interviewStatus.${value}`)}
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
                <TableHead>{t("interviews.when")}</TableHead>
                <TableHead>{t("candidates.fullName")}</TableHead>
                <TableHead>{t("jobs.title")}</TableHead>
                <TableHead>{t("jobs.client")}</TableHead>
                <TableHead>{t("interviews.round")}</TableHead>
                <TableHead>{t("interviews.mode")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {result.items.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>
                    <Link href={`/submissions/${row.submissionId}`}>{formatDate(row.scheduledAt)}</Link>
                  </TableCell>
                  <TableCell>{row.candidateName}</TableCell>
                  <TableCell>
                    {row.jobCode} · {row.jobTitle}
                  </TableCell>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>{row.roundNumber}</TableCell>
                  <TableCell>{tx(`enums.interviewMode.${row.mode}`)}</TableCell>
                  <TableCell>
                    <StatusPill kind="interviewStatus" value={row.status} />
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
