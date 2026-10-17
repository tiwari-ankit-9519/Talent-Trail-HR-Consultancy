"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Empty, ErrorBanner, Loading, Pager, StatusBadge } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { ClientSummary, Page } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";

export default function ClientsView() {
  const { can } = useAuth();
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<ClientSummary> | null>(null);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setPage(
        await api<Page<ClientSummary>>("/clients", {
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
        <h1>{t("clients.heading")}</h1>
        {can("CLIENTS_MANAGE") && (
          <Button type="button" onClick={() => router.push("/clients/new")}>
            {t("clients.add")}
          </Button>
        )}
      </div>
      <ErrorBanner error={error} />
      <div className="toolbar">
        <Input
          type="search"
          placeholder={t("clients.search")}
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
          <option value="ACTIVE">{t("common.active")}</option>
          <option value="INACTIVE">{t("common.inactive")}</option>
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
                <TableHead>{t("clients.name")}</TableHead>
                <TableHead>{t("clients.industry")}</TableHead>
                <TableHead>{t("clients.location")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {page.items.map((client) => (
                <TableRow key={client.id}>
                  <TableCell>
                    <Link href={`/clients/${client.id}`}>{client.name}</Link>
                  </TableCell>
                  <TableCell>{client.industry}</TableCell>
                  <TableCell>{[client.city, client.state].filter(Boolean).join(", ")}</TableCell>
                  <TableCell>
                    <StatusBadge status={client.status} />
                  </TableCell>
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
