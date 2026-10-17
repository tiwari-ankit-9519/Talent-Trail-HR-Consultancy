"use client";

import { useEffect, useState } from "react";
import PayslipDialog from "@/components/payroll/PayslipDialog";
import { useLive } from "@/components/LiveProvider";
import { Button } from "@/components/shadcn/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { formatMoney, t } from "@/lib/messages";
import type { Payslip } from "@/lib/types";

function monthTitle(month: string): string {
  const [year, number] = month.split("-").map(Number);
  return new Date(year, number - 1, 1).toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

export default function MyPayslipsView() {
  const [rows, setRows] = useState<Payslip[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [viewing, setViewing] = useState<Payslip | null>(null);

  function load() {
    api<Payslip[]>("/payroll/payslips/mine").then(setRows).catch(setError);
  }

  useEffect(load, []);
  useLive(["payroll"], load);

  return (
    <section>
      <div className="page-head">
        <h1>{t("payroll.mine.heading")}</h1>
      </div>
      <ErrorBanner error={error} />
      {!rows && !error && <Loading />}
      {rows && rows.length === 0 && <Empty />}
      {rows && rows.length > 0 && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("payroll.run.month")}</TableHead>
                <TableHead>{t("payroll.run.payableDays")}</TableHead>
                <TableHead>{t("payroll.run.net")}</TableHead>
                <TableHead>{t("common.actions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((slip) => (
                <TableRow key={slip.id}>
                  <TableCell>{monthTitle(slip.month)}</TableCell>
                  <TableCell>
                    {slip.payableDays} / {slip.calendarDays}
                  </TableCell>
                  <TableCell className="whitespace-nowrap font-semibold">{formatMoney(slip.net)}</TableCell>
                  <TableCell>
                    <Button type="button" size="sm" variant="outline" onClick={() => setViewing(slip)}>
                      {t("payroll.run.view")}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      <PayslipDialog slip={viewing} onClose={() => setViewing(null)} />
    </section>
  );
}
