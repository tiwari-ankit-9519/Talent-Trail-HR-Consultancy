"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import ExportButton from "@/components/reports/ExportButton";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { FormSelect } from "@/components/shadcn/select";
import { Input } from "@/components/shadcn/input";
import { Badge } from "@/components/shadcn/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";
import { Empty, ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { formatMinutes, formatTime, todayIso, toTimeInput } from "@/lib/attendance";
import { formatDay, t, tx } from "@/lib/messages";
import type { AttendanceStatus, AttendanceTeamDay, AttendanceTeamRow, WorkMode } from "@/lib/types";
import { DateField } from "@/components/shadcn/date-picker";
import { TimeField } from "@/components/shadcn/time-picker";

export default function TeamDayPanel() {
  const [date, setDate] = useState(todayIso());
  const [data, setData] = useState<AttendanceTeamDay | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [marking, setMarking] = useState<AttendanceTeamRow | null>(null);
  const [status, setStatus] = useState<AttendanceStatus>("PRESENT");
  const [mode, setMode] = useState<WorkMode>("OFFICE");
  const [checkIn, setCheckIn] = useState("");
  const [checkOut, setCheckOut] = useState("");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setData(await api<AttendanceTeamDay>("/attendance/team", { query: { date } }));
    } catch (failure) {
      setError(failure);
    }
  }, [date]);

  useEffect(() => {
    void load();
  }, [load]);

  function startMark(row: AttendanceTeamRow) {
    setError(null);
    setMarking(row);
    setStatus(row.state === "HALF_DAY" || row.state === "ABSENT" ? row.state : "PRESENT");
    setMode(row.workMode ?? "OFFICE");
    setCheckIn(toTimeInput(row.checkIn));
    setCheckOut(toTimeInput(row.checkOut));
    setNote(row.note ?? "");
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!marking) return;
    setBusy(true);
    setError(null);
    try {
      await api<void>("/attendance/team/mark", {
        method: "PUT",
        body: {
          userId: marking.userId,
          workDate: date,
          status,
          workMode: mode,
          checkIn: status === "ABSENT" ? null : checkIn || null,
          checkOut: status === "ABSENT" ? null : checkOut || null,
          note: note || null,
        },
      });
      setMarking(null);
      await load();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  const rows = data?.rows ?? [];
  const count = (state: string) => rows.filter((row) => row.state === state).length;
  const notMarked = rows.filter((row) => row.state === "NONE" || row.state === "ABSENT").length;

  return (
    <div className="grid gap-4">
      <div className="toolbar">
        <Field label={t("attendance.team.date")}>
          <DateField value={date} max={todayIso()} onChange={(e) => setDate(e.target.value || todayIso())} />
        </Field>
        <div className="ml-auto self-end">
          <ExportButton
            fileName={`attendance-${date}.csv`}
            disabled={rows.length === 0}
            headers={[
              t("attendance.team.name"),
              t("common.status"),
              t("attendance.today.workMode"),
              t("attendance.today.in"),
              t("attendance.today.out"),
              t("attendance.today.worked"),
              t("attendance.late"),
            ]}
            rows={rows.map((row) => [
              row.name,
              tx(`enums.attendanceState.${row.state}`),
              row.workMode ? tx(`enums.workMode.${row.workMode}`) : "",
              formatTime(row.checkIn),
              formatTime(row.checkOut),
              formatMinutes(row.workedMinutes),
              row.late ? t("common.yes") : "",
            ])}
          />
        </div>
      </div>
      <ErrorBanner error={error} />
      {!data && !error && <Loading />}
      {data && (
        <>
          <div className="flex flex-wrap items-center gap-2">
            <span className="muted small">{formatDay(data.date)}</span>
            {data.dayType !== "WORKING" && (
              <Badge variant="accent">{data.holidayName ?? tx(`enums.attendanceState.${data.dayType}`)}</Badge>
            )}
            <Badge variant="ok">{`${tx("enums.attendanceState.PRESENT")}: ${count("PRESENT")}`}</Badge>
            <Badge variant="warn">{`${tx("enums.attendanceState.HALF_DAY")}: ${count("HALF_DAY")}`}</Badge>
            <Badge variant="bad">{`${t("attendance.team.notIn")}: ${notMarked}`}</Badge>
          </div>
          {marking && (
            <Card className="p-5">
              <form onSubmit={submit} className="grid gap-4">
                <h2 className="m-0 text-lg">{t("attendance.team.markHeading").replace("{name}", marking.name)}</h2>
                <div className="form-grid">
                  <Field label={t("common.status")}>
                    <FormSelect value={status} onChange={(e) => setStatus(e.target.value as AttendanceStatus)}>
                      <option value="PRESENT">{t("enums.attendanceState.PRESENT")}</option>
                      <option value="HALF_DAY">{t("enums.attendanceState.HALF_DAY")}</option>
                      <option value="ABSENT">{t("enums.attendanceState.ABSENT")}</option>
                    </FormSelect>
                  </Field>
                  <Field label={t("attendance.today.workMode")}>
                    <FormSelect value={mode} onChange={(e) => setMode(e.target.value as WorkMode)}>
                      <option value="OFFICE">{t("enums.workMode.OFFICE")}</option>
                      <option value="WFH">{t("enums.workMode.WFH")}</option>
                    </FormSelect>
                  </Field>
                  <Field label={t("attendance.correction.checkIn")}>
                    <TimeField
                      value={checkIn}
                      disabled={status === "ABSENT"}
                      onChange={(e) => setCheckIn(e.target.value)}
                    />
                  </Field>
                  <Field label={t("attendance.correction.checkOut")}>
                    <TimeField
                      value={checkOut}
                      disabled={status === "ABSENT"}
                      onChange={(e) => setCheckOut(e.target.value)}
                    />
                  </Field>
                </div>
                <Field label={t("attendance.today.note")}>
                  <Input value={note} maxLength={300} onChange={(e) => setNote(e.target.value)} />
                </Field>
                <div className="flex gap-2">
                  <Button type="submit" disabled={busy}>
                    {t("common.save")}
                  </Button>
                  <Button type="button" variant="outline" onClick={() => setMarking(null)}>
                    {t("common.cancel")}
                  </Button>
                </div>
              </form>
            </Card>
          )}
          {rows.length === 0 ? (
            <Empty />
          ) : (
            <div className="table-wrap">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t("attendance.team.name")}</TableHead>
                    <TableHead>{t("common.status")}</TableHead>
                    <TableHead>{t("attendance.today.workMode")}</TableHead>
                    <TableHead>{t("attendance.today.in")}</TableHead>
                    <TableHead>{t("attendance.today.out")}</TableHead>
                    <TableHead>{t("attendance.today.worked")}</TableHead>
                    <TableHead>{t("attendance.team.source")}</TableHead>
                    <TableHead>{t("common.actions")}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.userId}>
                      <TableCell>
                        {row.name}
                        {row.late && (
                          <Badge variant="warn" className="ml-2">
                            {t("attendance.late")}
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell>
                        <StatusPill kind="attendanceState" value={row.state} />
                      </TableCell>
                      <TableCell>{row.workMode ? tx(`enums.workMode.${row.workMode}`) : ""}</TableCell>
                      <TableCell>{formatTime(row.checkIn)}</TableCell>
                      <TableCell>{formatTime(row.checkOut)}</TableCell>
                      <TableCell>{formatMinutes(row.workedMinutes)}</TableCell>
                      <TableCell>{row.source ? tx(`enums.attendanceSource.${row.source}`) : ""}</TableCell>
                      <TableCell>
                        <Button type="button" variant="outline" size="sm" onClick={() => startMark(row)}>
                          {t("attendance.team.mark")}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
