"use client";

import { Calculator, Lock } from "lucide-react";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import PayslipDialog from "@/components/payroll/PayslipDialog";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/shadcn/dialog";
import { Input } from "@/components/shadcn/input";
import { MonthField } from "@/components/shadcn/month-picker";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api, ApiError } from "@/lib/api";
import { currentMonthKey } from "@/lib/attendance";
import { confirmDialog } from "@/lib/confirm";
import { formatMoney, t } from "@/lib/messages";
import type { PayrollDetail, Payslip } from "@/lib/types";

type Draft = {
  slip: Payslip;
  otherEarnings: string;
  earningsNote: string;
  tds: string;
  otherDeductions: string;
  deductionsNote: string;
};

export default function PayrollRunPanel() {
  const [month, setMonth] = useState(currentMonthKey());
  const [detail, setDetail] = useState<PayrollDetail | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [viewing, setViewing] = useState<Payslip | null>(null);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [formError, setFormError] = useState<unknown>(null);

  const load = useCallback(async () => {
    setLoaded(false);
    setError(null);
    try {
      setDetail(await api<PayrollDetail>(`/payroll/runs/${month}`));
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 404) setDetail(null);
      else setError(failure);
    } finally {
      setLoaded(true);
    }
  }, [month]);

  useEffect(() => {
    void load();
  }, [load]);

  async function act(action: () => Promise<PayrollDetail>) {
    setBusy(true);
    setError(null);
    try {
      setDetail(await action());
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function calculate() {
    await act(() => api<PayrollDetail>("/payroll/runs", { method: "POST", body: { month } }));
  }

  async function finalize() {
    if (!(await confirmDialog(t("payroll.run.finalizeConfirm").replace("{month}", month)))) return;
    await act(() => api<PayrollDetail>(`/payroll/runs/${month}/finalize`, { method: "POST" }));
  }

  function openAdjust(slip: Payslip) {
    setFormError(null);
    setDraft({
      slip,
      otherEarnings: String(slip.otherEarnings),
      earningsNote: slip.earningsNote ?? "",
      tds: String(slip.tds),
      otherDeductions: String(slip.otherDeductions),
      deductionsNote: slip.deductionsNote ?? "",
    });
  }

  async function saveAdjust(event: FormEvent) {
    event.preventDefault();
    if (!draft) return;
    setBusy(true);
    setFormError(null);
    try {
      await api<Payslip>(`/payroll/payslips/${draft.slip.id}`, {
        method: "PUT",
        body: {
          otherEarnings: Number(draft.otherEarnings || 0),
          earningsNote: draft.earningsNote || null,
          tds: Number(draft.tds || 0),
          otherDeductions: Number(draft.otherDeductions || 0),
          deductionsNote: draft.deductionsNote || null,
          version: draft.slip.version,
        },
      });
      setDraft(null);
      await load();
    } catch (failure) {
      setFormError(failure);
    } finally {
      setBusy(false);
    }
  }

  const run = detail?.run;
  const locked = run?.status === "FINALIZED";

  return (
    <div className="grid gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <Field label={t("payroll.run.month")}>
          <MonthField value={month} onChange={(e) => setMonth(e.target.value)} />
        </Field>
        <div className="flex flex-wrap gap-2">
          {!locked && (
            <Button type="button" variant="outline" disabled={busy} onClick={() => void calculate()}>
              <Calculator aria-hidden="true" className="size-4" />
              {run ? t("payroll.run.recalculate") : t("payroll.run.calculate")}
            </Button>
          )}
          {run && !locked && (
            <Button type="button" disabled={busy} onClick={() => void finalize()}>
              <Lock aria-hidden="true" className="size-4" />
              {t("payroll.run.finalize")}
            </Button>
          )}
        </div>
      </div>
      <ErrorBanner error={error} />
      {!loaded && <Loading />}
      {loaded && !run && !error && <p className="muted m-0 text-sm">{t("payroll.run.notCalculated")}</p>}
      {run && detail && (
        <>
          <div className="grid grid-cols-2 gap-2 lg:grid-cols-5">
            <Card className="px-4 py-3">
              <p className="m-0 text-xs text-muted">{t("common.status")}</p>
              <p className="m-0 mt-1">
                <Badge variant={locked ? "ok" : "warn"}>
                  {locked ? t("payroll.run.statusFinalized") : t("payroll.run.statusDraft")}
                </Badge>
              </p>
            </Card>
            <Card className="px-4 py-3">
              <p className="m-0 text-xs text-muted">{t("payroll.run.employees")}</p>
              <p className="m-0 text-lg font-semibold">{run.employees}</p>
            </Card>
            <Card className="px-4 py-3">
              <p className="m-0 text-xs text-muted">{t("payroll.run.totalEarnings")}</p>
              <p className="m-0 text-lg font-semibold">{formatMoney(run.totalGross)}</p>
            </Card>
            <Card className="px-4 py-3">
              <p className="m-0 text-xs text-muted">{t("payroll.run.totalDeductions")}</p>
              <p className="m-0 text-lg font-semibold">{formatMoney(run.totalDeductions)}</p>
            </Card>
            <Card className="px-4 py-3">
              <p className="m-0 text-xs text-muted">{t("payroll.run.totalNet")}</p>
              <p className="m-0 text-lg font-semibold">{formatMoney(run.totalNet)}</p>
            </Card>
          </div>
          {!locked && <p className="muted m-0 text-sm">{t("payroll.run.draftHelp")}</p>}
          <div className="table-wrap">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("payroll.run.employee")}</TableHead>
                  <TableHead>{t("payroll.run.payableDays")}</TableHead>
                  <TableHead>{t("payroll.run.lossOfPay")}</TableHead>
                  <TableHead>{t("payroll.run.earnedGross")}</TableHead>
                  <TableHead>{t("payroll.run.commission")}</TableHead>
                  <TableHead>{t("payroll.run.deductions")}</TableHead>
                  <TableHead>{t("payroll.run.net")}</TableHead>
                  <TableHead>{t("common.actions")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {detail.payslips.map((slip) => (
                  <TableRow key={slip.id}>
                    <TableCell>{slip.employeeName}</TableCell>
                    <TableCell>
                      {slip.payableDays} / {slip.calendarDays}
                    </TableCell>
                    <TableCell>{slip.lossOfPayDays}</TableCell>
                    <TableCell>{formatMoney(slip.earnedGross)}</TableCell>
                    <TableCell>{formatMoney(slip.commission + slip.otherEarnings)}</TableCell>
                    <TableCell>{formatMoney(slip.totalDeductions)}</TableCell>
                    <TableCell className="whitespace-nowrap font-semibold">{formatMoney(slip.net)}</TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button type="button" size="sm" variant="outline" onClick={() => setViewing(slip)}>
                          {t("payroll.run.view")}
                        </Button>
                        {!locked && (
                          <Button type="button" size="sm" variant="outline" onClick={() => openAdjust(slip)}>
                            {t("payroll.run.adjust")}
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </>
      )}

      <PayslipDialog slip={viewing} onClose={() => setViewing(null)} />

      <Dialog open={draft !== null} onOpenChange={(open) => !open && setDraft(null)}>
        <DialogContent closeLabel={t("common.close")}>
          <DialogHeader>
            <DialogTitle>{t("payroll.adjust.title").replace("{name}", draft?.slip.employeeName ?? "")}</DialogTitle>
            <DialogDescription>{t("payroll.adjust.help")}</DialogDescription>
          </DialogHeader>
          {draft && (
            <form className="grid gap-3" onSubmit={(event) => void saveAdjust(event)}>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label={t("payroll.adjust.otherEarnings")}>
                  <Input
                    type="number"
                    min={0}
                    step="0.01"
                    value={draft.otherEarnings}
                    onChange={(e) => setDraft({ ...draft, otherEarnings: e.target.value })}
                  />
                </Field>
                <Field label={t("payroll.adjust.earningsNote")}>
                  <Input
                    maxLength={200}
                    value={draft.earningsNote}
                    onChange={(e) => setDraft({ ...draft, earningsNote: e.target.value })}
                  />
                </Field>
                <Field label={t("payroll.adjust.tds")}>
                  <Input
                    type="number"
                    min={0}
                    step="0.01"
                    value={draft.tds}
                    onChange={(e) => setDraft({ ...draft, tds: e.target.value })}
                  />
                </Field>
                <Field label={t("payroll.adjust.otherDeductions")}>
                  <Input
                    type="number"
                    min={0}
                    step="0.01"
                    value={draft.otherDeductions}
                    onChange={(e) => setDraft({ ...draft, otherDeductions: e.target.value })}
                  />
                </Field>
              </div>
              <Field label={t("payroll.adjust.deductionsNote")}>
                <Input
                  maxLength={200}
                  value={draft.deductionsNote}
                  onChange={(e) => setDraft({ ...draft, deductionsNote: e.target.value })}
                />
              </Field>
              <ErrorBanner error={formError} />
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setDraft(null)}>
                  {t("common.cancel")}
                </Button>
                <Button type="submit" disabled={busy}>
                  {t("common.save")}
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
