"use client";

import { useCallback, useEffect, useState } from "react";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { confirmDialog } from "@/lib/confirm";
import { formatDay, t, tx } from "@/lib/messages";
import type { LeaveRow } from "@/lib/types";

type Deciding = { id: string; kind: "approve" | "reject" } | null;

export default function LeaveRequestsPanel() {
  const [status, setStatus] = useState("PENDING");
  const [rows, setRows] = useState<LeaveRow[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [deciding, setDeciding] = useState<Deciding>(null);
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setRows(await api<LeaveRow[]>("/attendance/leave/requests", { query: { status: status || undefined } }));
    } catch (failure) {
      setError(failure);
    }
  }, [status]);

  useEffect(() => {
    void load();
  }, [load]);

  async function confirmDecision(row: LeaveRow) {
    if (!deciding) return;
    setBusy(true);
    setError(null);
    try {
      await api<void>(`/attendance/leave/${row.id}/${deciding.kind}`, {
        method: "POST",
        body: { note: note || null },
      });
      setDeciding(null);
      setNote("");
      await load();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function cancel(row: LeaveRow) {
    if (!(await confirmDialog(t("attendance.leaveTeam.cancelConfirm").replace("{name}", row.userName)))) return;
    setError(null);
    try {
      await api<void>(`/attendance/leave/${row.id}`, { method: "DELETE" });
      await load();
    } catch (failure) {
      setError(failure);
    }
  }

  return (
    <div className="grid gap-4">
      <div className="toolbar">
        <FormSelect value={status} aria-label={t("common.status")} onChange={(e) => setStatus(e.target.value)}>
          <option value="">{t("common.allStatuses")}</option>
          <option value="PENDING">{t("enums.requestStatus.PENDING")}</option>
          <option value="APPROVED">{t("enums.requestStatus.APPROVED")}</option>
          <option value="REJECTED">{t("enums.requestStatus.REJECTED")}</option>
          <option value="CANCELLED">{t("enums.requestStatus.CANCELLED")}</option>
        </FormSelect>
      </div>
      <ErrorBanner error={error} />
      {!rows && !error && <Loading />}
      {rows && rows.length === 0 && <Empty />}
      {rows && rows.length > 0 && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("attendance.team.name")}</TableHead>
                <TableHead>{t("attendance.requests.date")}</TableHead>
                <TableHead>{t("attendance.leaveTeam.type")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("attendance.requests.decision")}</TableHead>
                <TableHead>{t("common.actions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{row.userName}</TableCell>
                  <TableCell>{formatDay(row.date)}</TableCell>
                  <TableCell>
                    <Badge variant="accent">{tx(`enums.leaveKind.${row.kind}`)}</Badge>
                  </TableCell>
                  <TableCell>
                    <StatusPill kind="requestStatus" value={row.status} />
                  </TableCell>
                  <TableCell>
                    {row.decidedBy ? `${row.decidedBy}${row.decisionNote ? `: ${row.decisionNote}` : ""}` : ""}
                  </TableCell>
                  <TableCell>
                    {row.status === "PENDING" && deciding?.id === row.id ? (
                      <div className="flex flex-wrap items-center gap-2">
                        <Input
                          value={note}
                          maxLength={300}
                          placeholder={t("attendance.requests.notePlaceholder")}
                          onChange={(e) => setNote(e.target.value)}
                          className="min-w-40"
                        />
                        <Button
                          type="button"
                          size="sm"
                          variant={deciding.kind === "reject" ? "destructive" : "default"}
                          disabled={busy}
                          onClick={() => void confirmDecision(row)}
                        >
                          {t("common.confirm")}
                        </Button>
                        <Button type="button" size="sm" variant="outline" onClick={() => setDeciding(null)}>
                          {t("common.cancel")}
                        </Button>
                      </div>
                    ) : row.status === "PENDING" ? (
                      <div className="flex gap-2">
                        <Button
                          type="button"
                          size="sm"
                          onClick={() => {
                            setNote("");
                            setDeciding({ id: row.id, kind: "approve" });
                          }}
                        >
                          {t("attendance.requests.approve")}
                        </Button>
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            setNote("");
                            setDeciding({ id: row.id, kind: "reject" });
                          }}
                        >
                          {t("attendance.requests.reject")}
                        </Button>
                      </div>
                    ) : row.status === "APPROVED" ? (
                      <Button type="button" size="sm" variant="outline" onClick={() => void cancel(row)}>
                        {t("common.cancel")}
                      </Button>
                    ) : null}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
