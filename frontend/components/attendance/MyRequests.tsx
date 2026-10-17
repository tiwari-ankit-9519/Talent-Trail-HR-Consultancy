"use client";

import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import StatusPill from "@/components/StatusPill";
import { Empty } from "@/components/ui";
import { formatDay, t, tx } from "@/lib/messages";
import type { AttendanceCorrection } from "@/lib/types";

export default function MyRequests({ rows }: { rows: AttendanceCorrection[] }) {
  return (
    <section className="section-block">
      <h2>{t("attendance.requests.mine")}</h2>
      {rows.length === 0 ? (
        <Empty />
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("attendance.requests.date")}</TableHead>
                <TableHead>{t("attendance.requests.times")}</TableHead>
                <TableHead>{t("attendance.today.workMode")}</TableHead>
                <TableHead>{t("attendance.correction.reason")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("attendance.requests.decision")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
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
                    {row.decidedBy ? `${row.decidedBy}${row.decisionNote ? `: ${row.decisionNote}` : ""}` : ""}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  );
}
