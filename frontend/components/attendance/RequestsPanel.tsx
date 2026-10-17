"use client";

import { useCallback, useEffect, useState } from "react";
import { useLive } from "@/components/LiveProvider";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDay, t, tx } from "@/lib/messages";
import type { AttendanceCorrection } from "@/lib/types";

type Deciding = { id: string; kind: "approve" | "reject" } | null;

export default function RequestsPanel({ onDecided }: { onDecided?: () => void }) {
  const [status, setStatus] = useState("PENDING");
  const [rows, setRows] = useState<AttendanceCorrection[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [deciding, setDeciding] = useState<Deciding>(null);
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setRows(await api<AttendanceCorrection[]>("/attendance/requests", { query: { status: status || undefined } }));
    } catch (failure) {
      setError(failure);
    }
  }, [status]);

  useLive(["correction"], () => void load());

  useEffect(() => {
    void load();
  }, [load]);

  async function confirm(row: AttendanceCorrection) {
    if (!deciding) return;
    setBusy(true);
    setError(null);
    try {
      await api<AttendanceCorrection>(`/attendance/requests/${row.id}/${deciding.kind}`, {
        method: "POST",
        body: { note: note || null, version: row.version },
      });
      setDeciding(null);
      setNote("");
      await load();
      onDecided?.();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
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
                <TableHead>{t("attendance.requests.times")}</TableHead>
                <TableHead>{t("attendance.today.workMode")}</TableHead>
                <TableHead>{t("attendance.correction.reason")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("common.actions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{row.userName}</TableCell>
                  <TableCell>{formatDay(row.workDate)}</TableCell>
                  <TableCell>
                    {row.checkIn}
                    {row.checkOut ? ` – ${row.checkOut}` : ""}
                  </TableCell>
                  <TableCell>{tx(`enums.workMode.${row.workMode}`)}</TableCell>
                  <TableCell>{row.reason}</TableCell>
                  <TableCell>
                    <StatusPill kind="requestStatus" value={row.status} />
                  </TableCell>
                  <TableCell>
                    {row.status !== "PENDING" ? (
                      <span className="muted small">
                        {row.decidedBy}
                        {row.decisionNote ? `: ${row.decisionNote}` : ""}
                      </span>
                    ) : deciding?.id === row.id ? (
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
                          onClick={() => void confirm(row)}
                        >
                          {t("common.confirm")}
                        </Button>
                        <Button type="button" size="sm" variant="outline" onClick={() => setDeciding(null)}>
                          {t("common.cancel")}
                        </Button>
                      </div>
                    ) : (
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
                    )}
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
