"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Empty, ErrorBanner, Loading, Pager } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDay, formatMoney, t, tx } from "@/lib/messages";
import type { InvoiceStatus, InvoiceSummary, Page } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Badge } from "@/components/shadcn/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";

const statuses: InvoiceStatus[] = ["ISSUED", "PARTIALLY_PAID", "PAID", "CANCELLED"];

export default function InvoicesView() {
  const [status, setStatus] = useState("");
  const [overdue, setOverdue] = useState(false);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<Page<InvoiceSummary> | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setResult(
        await api<Page<InvoiceSummary>>("/invoices", {
          query: { status: status || undefined, overdue: overdue || undefined, q: query || undefined, page, size: 20 },
        }),
      );
    } catch (failure) {
      setError(failure);
    }
  }, [status, overdue, query, page]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section>
      <h1>{t("invoices.pageHeading")}</h1>
      <form
        className="toolbar"
        onSubmit={(e) => {
          e.preventDefault();
          setPage(0);
          setQuery(search.trim());
        }}
      >
        <Input
          type="search"
          value={search}
          placeholder={t("invoices.searchPlaceholder")}
          aria-label={t("invoices.searchPlaceholder")}
          onChange={(e) => setSearch(e.target.value)}
        />
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
              {tx(`enums.invoiceStatus.${value}`)}
            </option>
          ))}
        </FormSelect>
        <label className="check">
          <CheckboxField
            checked={overdue}
            onChange={(e) => {
              setPage(0);
              setOverdue(e.target.checked);
            }}
          />
          {t("invoices.overdueOnly")}
        </label>
        <Button variant="outline" type="submit">
          {t("invoices.search")}
        </Button>
      </form>
      <ErrorBanner error={error} />
      {!result && !error && <Loading />}
      {result && result.items.length === 0 && <Empty />}
      {result && result.items.length > 0 && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("invoices.number")}</TableHead>
                <TableHead>{t("jobs.client")}</TableHead>
                <TableHead>{t("candidates.fullName")}</TableHead>
                <TableHead>{t("invoices.invoiceDate")}</TableHead>
                <TableHead>{t("invoices.dueDate")}</TableHead>
                <TableHead>{t("invoices.total")}</TableHead>
                <TableHead>{t("invoices.balance")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {result.items.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>
                    <Link href={`/invoices/${row.id}`}>{row.number}</Link>
                  </TableCell>
                  <TableCell>{row.clientName}</TableCell>
                  <TableCell>
                    {row.candidateName} · {row.jobCode}
                  </TableCell>
                  <TableCell>{formatDay(row.invoiceDate)}</TableCell>
                  <TableCell>{formatDay(row.dueDate)}</TableCell>
                  <TableCell>{formatMoney(row.totalAmount)}</TableCell>
                  <TableCell>{formatMoney(row.balance)}</TableCell>
                  <TableCell>
                    <div className="flex flex-wrap items-center gap-1.5">
                      <StatusPill kind="invoiceStatus" value={row.status} />
                      {row.overdue && <Badge variant="bad">{t("invoices.overdue")}</Badge>}
                    </div>
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
