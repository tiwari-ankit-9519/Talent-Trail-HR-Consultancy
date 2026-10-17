"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Empty, ErrorBanner, Loading, Pager } from "@/components/ui";
import { api } from "@/lib/api";
import { t, tx } from "@/lib/messages";
import type { CandidateSource, CandidateStatus, CandidateSummary, ConsentStatus, Page } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { badgeVariants } from "@/components/shadcn/badge";
import StatusPill from "@/components/StatusPill";

const statuses: CandidateStatus[] = ["NEW", "SCREENING", "AVAILABLE", "NOT_INTERESTED", "PLACED", "BLOCKED"];
const consents: ConsentStatus[] = ["NOT_ASKED", "GRANTED", "DENIED", "WITHDRAWN"];
const sources: CandidateSource[] = ["LINKEDIN", "NAUKRI", "APPLY_LINK", "REFERRAL", "OTHER"];

export default function CandidatesView() {
  const { can } = useAuth();
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [consent, setConsent] = useState("");
  const [source, setSource] = useState("");
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<CandidateSummary> | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setPage(
        await api<Page<CandidateSummary>>("/candidates", {
          query: { q: query, status, consent, source, page: pageNumber, size: 20 },
        }),
      );
    } catch (failure) {
      setError(failure);
    }
  }, [query, status, consent, source, pageNumber]);

  useEffect(() => {
    void load();
  }, [load]);

  function reset(setter: (value: string) => void) {
    return (value: string) => {
      setPageNumber(0);
      setter(value);
    };
  }

  return (
    <section>
      <div className="page-head">
        <h1>{t("candidates.heading")}</h1>
        {can("CANDIDATES_MANAGE") && (
          <Button type="button" onClick={() => router.push("/candidates/new")}>
            {t("candidates.add")}
          </Button>
        )}
      </div>
      <ErrorBanner error={error} />
      <div className="toolbar">
        <Input
          type="search"
          placeholder={t("candidates.search")}
          value={query}
          onChange={(e) => reset(setQuery)(e.target.value)}
        />
        <FormSelect value={status} onChange={(e) => reset(setStatus)(e.target.value)}>
          <option value="">{t("common.allStatuses")}</option>
          {statuses.map((value) => (
            <option key={value} value={value}>
              {tx(`enums.candidateStatus.${value}`)}
            </option>
          ))}
        </FormSelect>
        <FormSelect value={consent} onChange={(e) => reset(setConsent)(e.target.value)}>
          <option value="">{t("candidates.anyConsent")}</option>
          {consents.map((value) => (
            <option key={value} value={value}>
              {tx(`enums.consentStatus.${value}`)}
            </option>
          ))}
        </FormSelect>
        <FormSelect value={source} onChange={(e) => reset(setSource)(e.target.value)}>
          <option value="">{t("candidates.anySource")}</option>
          {sources.map((value) => (
            <option key={value} value={value}>
              {tx(`enums.candidateSource.${value}`)}
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
                <TableHead>{t("candidates.fullName")}</TableHead>
                <TableHead>{t("candidates.email")}</TableHead>
                <TableHead>{t("candidates.phone")}</TableHead>
                <TableHead>{t("candidates.currentTitle")}</TableHead>
                <TableHead>{t("candidates.experience")}</TableHead>
                <TableHead>{t("candidates.source")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("candidates.consent")}</TableHead>
                <TableHead>{t("candidates.resume")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {page.items.map((candidate) => (
                <TableRow key={candidate.id}>
                  <TableCell>
                    <Link href={`/candidates/${candidate.id}`}>{candidate.fullName}</Link>
                  </TableCell>
                  <TableCell>{candidate.email}</TableCell>
                  <TableCell>{candidate.phone}</TableCell>
                  <TableCell>
                    {candidate.currentTitle}
                    {candidate.currentCompany ? ` · ${candidate.currentCompany}` : ""}
                  </TableCell>
                  <TableCell>{candidate.totalExperienceYears}</TableCell>
                  <TableCell>{tx(`enums.candidateSource.${candidate.source}`)}</TableCell>
                  <TableCell>
                    <StatusPill kind="candidateStatus" value={candidate.status} />
                  </TableCell>
                  <TableCell>
                    <span
                      className={
                        candidate.consentStatus === "GRANTED"
                          ? badgeVariants({ variant: "ok" })
                          : candidate.consentStatus === "NOT_ASKED"
                            ? badgeVariants()
                            : badgeVariants({ variant: "warn" })
                      }
                    >
                      {tx(`enums.consentStatus.${candidate.consentStatus}`)}
                    </span>
                  </TableCell>
                  <TableCell>{candidate.hasResume ? t("common.yes") : t("common.no")}</TableCell>
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
