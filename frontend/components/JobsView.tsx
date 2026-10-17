"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Empty, ErrorBanner, Loading, Pager } from "@/components/ui";
import { api } from "@/lib/api";
import { t, tx } from "@/lib/messages";
import type { JobStatus, JobSummary, Page } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";

const statuses: JobStatus[] = ["DRAFT", "OPEN", "ON_HOLD", "CLOSED"];

export default function JobsView() {
  const { can } = useAuth();
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<JobSummary> | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setPage(
        await api<Page<JobSummary>>("/jobs", {
          query: { q: query, status, page: pageNumber, size: 20 },
        }),
      );
    } catch (failure) {
      setError(failure);
    }
  }, [query, status, pageNumber]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section>
      <div className="page-head">
        <h1>{t("jobs.heading")}</h1>
        {can("JOBS_MANAGE") && (
          <Button type="button" onClick={() => router.push("/jobs/new")}>
            {t("jobs.add")}
          </Button>
        )}
      </div>
      <ErrorBanner error={error} />
      <div className="toolbar">
        <Input
          type="search"
          placeholder={t("jobs.search")}
          value={query}
          onChange={(e) => {
            setPageNumber(0);
            setQuery(e.target.value);
          }}
        />
        <FormSelect
          value={status}
          onChange={(e) => {
            setPageNumber(0);
            setStatus(e.target.value);
          }}
        >
          <option value="">{t("common.allStatuses")}</option>
          {statuses.map((value) => (
            <option key={value} value={value}>
              {tx(`enums.jobStatus.${value}`)}
            </option>
          ))}
        </FormSelect>
      </div>
      {!page ? (
        <Loading />
      ) : page.items.length === 0 ? (
        <Empty />
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("jobs.code")}</TableHead>
                <TableHead>{t("jobs.title")}</TableHead>
                <TableHead>{t("jobs.client")}</TableHead>
                <TableHead>{t("jobs.location")}</TableHead>
                <TableHead>{t("jobs.openings")}</TableHead>
                <TableHead>{t("jobs.applicants")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("jobs.published")}</TableHead>
                <TableHead>{t("jobs.closingDate")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {page.items.map((job) => (
                <TableRow key={job.id}>
                  <TableCell>{job.code}</TableCell>
                  <TableCell>
                    <Link href={`/jobs/${job.id}`}>{job.title}</Link>
                  </TableCell>
                  <TableCell>{job.clientName}</TableCell>
                  <TableCell>{job.location}</TableCell>
                  <TableCell>{job.openings}</TableCell>
                  <TableCell>{job.applicantCount}</TableCell>
                  <TableCell>
                    <StatusPill kind="jobStatus" value={job.status} />
                  </TableCell>
                  <TableCell>{job.published ? t("common.yes") : t("common.no")}</TableCell>
                  <TableCell>{job.closingDate ?? ""}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      <Pager page={page} onChange={setPageNumber} />
    </section>
  );
}
