"use client";

import { useEffect, useState } from "react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t, tx } from "@/lib/messages";
import type { ReminderLogEntry } from "@/lib/types";

export default function ReminderHistoryPanel() {
  const [rows, setRows] = useState<ReminderLogEntry[] | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    void (async () => {
      try {
        setRows(await api<ReminderLogEntry[]>("/reminders/log"));
      } catch (failure) {
        setError(failure);
      }
    })();
  }, []);

  if (!rows) {
    return (
      <div className="mt-4">
        <ErrorBanner error={error} />
        {!error && <Loading />}
      </div>
    );
  }
  if (rows.length === 0) return <Empty />;

  return (
    <div className="mt-4 overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t("emails.history.sentAt")}</TableHead>
            <TableHead>{t("emails.history.reminder")}</TableHead>
            <TableHead>{t("emails.history.subject")}</TableHead>
            <TableHead>{t("emails.history.to")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.map((row) => (
            <TableRow key={row.id}>
              <TableCell>{formatDate(row.sentAt)}</TableCell>
              <TableCell>{tx(`emails.reminderNames.${row.code}`, row.code)}</TableCell>
              <TableCell>{row.subject}</TableCell>
              <TableCell>{row.recipients}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
