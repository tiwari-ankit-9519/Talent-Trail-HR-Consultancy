"use client";

import { Button } from "@/components/shadcn/button";
import { useCallback, useEffect, useState } from "react";
import { Empty, ErrorBanner, Loading, Pager } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t } from "@/lib/messages";
import type { AuditRow, Page } from "@/lib/types";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { badgeVariants } from "@/components/shadcn/badge";
import { DateTimeField } from "@/components/shadcn/datetime-picker";

export default function AuditView() {
  const [action, setAction] = useState("");
  const [entityType, setEntityType] = useState("");
  const [actorEmail, setActorEmail] = useState("");
  const [outcome, setOutcome] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<AuditRow> | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [openId, setOpenId] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setPage(
        await api<Page<AuditRow>>("/audit-logs", {
          query: {
            action,
            entityType,
            actorEmail,
            outcome,
            from: from ? new Date(from).toISOString() : undefined,
            to: to ? new Date(to).toISOString() : undefined,
            page: pageNumber,
            size: 25,
          },
        }),
      );
    } catch (failure) {
      setError(failure);
    }
  }, [action, entityType, actorEmail, outcome, from, to, pageNumber]);

  useEffect(() => {
    void load();
  }, [load]);

  function reset<T>(setter: (value: T) => void) {
    return (value: T) => {
      setPageNumber(0);
      setter(value);
    };
  }

  return (
    <section>
      <h1>{t("audit.heading")}</h1>
      <ErrorBanner error={error} />
      <div className="toolbar">
        <Input
          placeholder={t("audit.action")}
          value={action}
          onChange={(e) => reset(setAction)(e.target.value.toUpperCase())}
        />
        <Input
          placeholder={t("audit.entityType")}
          value={entityType}
          onChange={(e) => reset(setEntityType)(e.target.value.toUpperCase())}
        />
        <Input
          placeholder={t("audit.actor")}
          value={actorEmail}
          onChange={(e) => reset(setActorEmail)(e.target.value)}
        />
        <FormSelect value={outcome} onChange={(e) => reset(setOutcome)(e.target.value)}>
          <option value="">{t("audit.anyOutcome")}</option>
          <option value="SUCCESS">{t("audit.success")}</option>
          <option value="FAILURE">{t("audit.failure")}</option>
        </FormSelect>
        <label className="inline-field">
          {t("audit.from")}
          <DateTimeField value={from} onChange={(e) => reset(setFrom)(e.target.value)} />
        </label>
        <label className="inline-field">
          {t("audit.to")}
          <DateTimeField value={to} onChange={(e) => reset(setTo)(e.target.value)} />
        </label>
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
                <TableHead>{t("audit.when")}</TableHead>
                <TableHead>{t("audit.actor")}</TableHead>
                <TableHead>{t("audit.action")}</TableHead>
                <TableHead>{t("audit.summary")}</TableHead>
                <TableHead>{t("audit.outcome")}</TableHead>
                <TableHead>{t("audit.ip")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {page.items.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{formatDate(row.occurredAt)}</TableCell>
                  <TableCell>{row.actorEmail}</TableCell>
                  <TableCell>{row.action}</TableCell>
                  <TableCell>
                    {row.summary}
                    {row.details && (
                      <>
                        {" "}
                        <Button
                          type="button"
                          variant="link"
                          className="h-auto p-0"
                          onClick={() => setOpenId(openId === row.id ? null : row.id)}
                        >
                          {openId === row.id ? t("audit.hideDetails") : t("audit.showDetails")}
                        </Button>
                        {openId === row.id && <pre className="details">{row.details}</pre>}
                      </>
                    )}
                  </TableCell>
                  <TableCell>
                    <span
                      className={
                        row.outcome === "SUCCESS"
                          ? badgeVariants({ variant: "ok" })
                          : badgeVariants({ variant: "warn" })
                      }
                    >
                      {row.outcome === "SUCCESS" ? t("audit.success") : t("audit.failure")}
                    </span>
                  </TableCell>
                  <TableCell>{row.ipAddress}</TableCell>
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
