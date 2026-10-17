"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Empty, ErrorBanner, Loading, Pager } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t, tx } from "@/lib/messages";
import type { Page, SubmissionStatus, SubmissionSummary } from "@/lib/types";
import { FormSelect } from "@/components/shadcn/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";

const statuses: SubmissionStatus[] = [
  "SUBMITTED",
  "SHORTLISTED",
  "INTERVIEWING",
  "SELECTED",
  "ON_HOLD",
  "REJECTED",
  "WITHDRAWN",
];

type Props = { jobId?: string; candidateId?: string; refreshKey?: number };

export default function SubmissionsView({ jobId, candidateId, refreshKey = 0 }: Props) {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<Page<SubmissionSummary> | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setResult(
        await api<Page<SubmissionSummary>>("/submissions", {
          query: { status: status || undefined, jobId, candidateId, page, size: 20 },
        }),
      );
    } catch (failure) {
      setError(failure);
    }
  }, [status, jobId, candidateId, page, refreshKey]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div>
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
              {tx(`enums.submissionStatus.${value}`)}
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
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("submissions.email")}</TableHead>
                <TableHead>{t("submissions.submittedAt")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {result.items.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>
                    <Link href={`/submissions/${row.id}`}>{row.candidateName}</Link>
                  </TableCell>
                  <TableCell>
                    {row.jobCode} · {row.jobTitle}
                  </TableCell>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>
                    <StatusPill kind="submissionStatus" value={row.status} />
                  </TableCell>
                  <TableCell>
                    <StatusPill kind="emailStatus" value={row.emailStatus} />
                  </TableCell>
                  <TableCell>{formatDate(row.submittedAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      <Pager page={result} onChange={setPage} />
    </div>
  );
}
