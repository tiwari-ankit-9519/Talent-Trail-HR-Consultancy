"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Checkbox } from "@/components/shadcn/checkbox";
import { DateField } from "@/components/shadcn/date-picker";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/shadcn/dialog";
import { Input } from "@/components/shadcn/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { todayIso } from "@/lib/attendance";
import { formatDay, formatMoney, t } from "@/lib/messages";
import type { PayrollBreakdown, PayrollEmployee, PayrollStructure } from "@/lib/types";

type Draft = {
  employee: PayrollEmployee;
  effectiveFrom: string;
  ctcAnnual: string;
  variablePercent: string;
  pfApplicable: boolean;
  ptApplicable: boolean;
  note: string;
};

function Line({ label, value, strong }: { label: string; value: number; strong?: boolean }) {
  return (
    <div className={`flex items-center justify-between gap-3 text-sm ${strong ? "font-semibold" : ""}`}>
      <span className={strong ? "" : "text-muted"}>{label}</span>
      <span className="tabular-nums">{formatMoney(value)}</span>
    </div>
  );
}

export default function StructuresPanel() {
  const [rows, setRows] = useState<PayrollEmployee[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [preview, setPreview] = useState<PayrollBreakdown | null>(null);
  const [history, setHistory] = useState<PayrollStructure[]>([]);
  const [formError, setFormError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setRows(await api<PayrollEmployee[]>("/payroll/employees"));
    } catch (failure) {
      setError(failure);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const ctc = draft?.ctcAnnual ?? "";
  const variable = draft?.variablePercent ?? "";
  const pf = draft?.pfApplicable ?? true;
  const isOpen = draft !== null;

  useEffect(() => {
    if (!isOpen || ctc === "" || Number(ctc) <= 0) {
      setPreview(null);
      return;
    }
    let cancelled = false;
    api<PayrollBreakdown>("/payroll/structures/preview", {
      method: "POST",
      body: { ctcAnnual: Number(ctc), variablePercent: Number(variable || 0), pfApplicable: pf },
    })
      .then((result) => {
        if (!cancelled) {
          setPreview(result);
          setFormError(null);
        }
      })
      .catch((failure) => {
        if (!cancelled) {
          setPreview(null);
          setFormError(failure);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [isOpen, ctc, variable, pf]);

  async function open(employee: PayrollEmployee) {
    setFormError(null);
    const current = employee.current;
    setDraft({
      employee,
      effectiveFrom: todayIso().slice(0, 8) + "01",
      ctcAnnual: current?.ctcAnnual != null ? String(current.ctcAnnual) : "",
      variablePercent: current?.variablePercent != null ? String(current.variablePercent) : "0",
      pfApplicable: current ? current.pfApplicable : true,
      ptApplicable: current ? current.ptApplicable : true,
      note: "",
    });
    try {
      setHistory(await api<PayrollStructure[]>(`/payroll/employees/${employee.userId}/structures`));
    } catch {
      setHistory([]);
    }
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!draft) return;
    setBusy(true);
    setFormError(null);
    try {
      await api<PayrollStructure>(`/payroll/employees/${draft.employee.userId}/structure`, {
        method: "PUT",
        body: {
          effectiveFrom: draft.effectiveFrom,
          ctcAnnual: Number(draft.ctcAnnual),
          variablePercent: Number(draft.variablePercent || 0),
          pfApplicable: draft.pfApplicable,
          ptApplicable: draft.ptApplicable,
          note: draft.note || null,
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

  return (
    <div className="grid gap-4">
      <p className="muted m-0 max-w-3xl text-sm">{t("payroll.structures.help")}</p>
      <ErrorBanner error={error} />
      {!rows && !error && <Loading />}
      {rows && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("payroll.run.employee")}</TableHead>
                <TableHead>{t("payroll.structures.ctc")}</TableHead>
                <TableHead>{t("payroll.structures.variable")}</TableHead>
                <TableHead>{t("payroll.structures.gross")}</TableHead>
                <TableHead>{t("payroll.structures.breakup")}</TableHead>
                <TableHead>{t("payroll.structures.since")}</TableHead>
                <TableHead>{t("common.actions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.userId}>
                  <TableCell>
                    {row.name} {!row.active && <Badge variant="default">{t("common.inactive")}</Badge>}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {row.current?.ctcAnnual != null ? formatMoney(row.current.ctcAnnual) : "-"}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {row.current?.variableAnnual != null
                      ? `${formatMoney(row.current.variableAnnual)} (${row.current.variablePercent}%)`
                      : "-"}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {row.current ? formatMoney(row.current.gross) : t("payroll.structures.notSet")}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {row.current?.basic != null
                      ? `${formatMoney(row.current.basic)} / ${formatMoney(row.current.hra ?? 0)} / ${formatMoney(row.current.allowance ?? 0)}`
                      : "-"}
                  </TableCell>
                  <TableCell>{row.current ? formatDay(row.current.effectiveFrom) : "-"}</TableCell>
                  <TableCell>
                    <Button type="button" size="sm" variant="outline" onClick={() => void open(row)}>
                      {row.current ? t("payroll.structures.revise") : t("payroll.structures.set")}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog open={draft !== null} onOpenChange={(value) => !value && setDraft(null)}>
        <DialogContent closeLabel={t("common.close")} className="max-w-xl">
          <DialogHeader>
            <DialogTitle>
              {t("payroll.structures.dialogTitle").replace("{name}", draft?.employee.name ?? "")}
            </DialogTitle>
            <DialogDescription>{t("payroll.structures.dialogHelp")}</DialogDescription>
          </DialogHeader>
          {draft && (
            <form className="grid gap-3" onSubmit={(event) => void save(event)}>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label={t("payroll.structures.effectiveFrom")}>
                  <DateField
                    required
                    value={draft.effectiveFrom}
                    onChange={(e) => setDraft({ ...draft, effectiveFrom: e.target.value })}
                  />
                </Field>
                <Field label={t("payroll.structures.ctcAnnual")}>
                  <Input
                    required
                    type="number"
                    min={1}
                    step="1"
                    value={draft.ctcAnnual}
                    onChange={(e) => setDraft({ ...draft, ctcAnnual: e.target.value })}
                  />
                </Field>
                <Field label={t("payroll.structures.variablePercent")} hint={t("payroll.structures.variableHint")}>
                  <Input
                    required
                    type="number"
                    min={0}
                    max={100}
                    step="0.01"
                    value={draft.variablePercent}
                    onChange={(e) => setDraft({ ...draft, variablePercent: e.target.value })}
                  />
                </Field>
              </div>
              <div className="flex flex-wrap gap-4">
                <label className="flex cursor-pointer items-center gap-2 text-sm">
                  <Checkbox
                    checked={draft.pfApplicable}
                    onCheckedChange={(checked) => setDraft({ ...draft, pfApplicable: checked === true })}
                  />
                  {t("payroll.structures.pfApplicable")}
                </label>
                <label className="flex cursor-pointer items-center gap-2 text-sm">
                  <Checkbox
                    checked={draft.ptApplicable}
                    onCheckedChange={(checked) => setDraft({ ...draft, ptApplicable: checked === true })}
                  />
                  {t("payroll.structures.ptApplicable")}
                </label>
              </div>
              {preview && (
                <div className="grid gap-1.5 rounded-lg border border-line bg-bg p-4">
                  <p className="m-0 text-xs font-semibold uppercase tracking-wide text-muted">
                    {t("payroll.structures.breakdown")}
                  </p>
                  <Line label={t("payroll.structures.fixedAnnual")} value={preview.fixedAnnual} />
                  <Line label={t("payroll.structures.variableAnnual")} value={preview.variableAnnual} />
                  <div className="my-1 border-t border-line" />
                  <Line label={t("payroll.payslip.basic")} value={preview.basic} />
                  <Line label={t("payroll.payslip.hra")} value={preview.hra} />
                  <Line label={t("payroll.structures.specialAllowance")} value={preview.allowance} />
                  <Line label={t("payroll.structures.grossMonthly")} value={preview.gross} strong />
                  <div className="my-1 border-t border-line" />
                  <Line label={t("payroll.structures.employerPf")} value={preview.employerPf} />
                  <Line label={t("payroll.structures.gratuity")} value={preview.gratuity} />
                  <p className="muted m-0 mt-1 text-xs">{t("payroll.structures.breakdownHelp")}</p>
                </div>
              )}
              <Field label={t("payroll.structures.note")}>
                <Input
                  maxLength={200}
                  value={draft.note}
                  onChange={(e) => setDraft({ ...draft, note: e.target.value })}
                />
              </Field>
              {history.length > 0 && (
                <div className="grid gap-1">
                  <p className="m-0 text-xs font-semibold text-muted">{t("payroll.structures.history")}</p>
                  <ul className="m-0 grid list-none gap-1 p-0 text-sm">
                    {history.slice(0, 5).map((item) => (
                      <li key={item.id} className="flex justify-between gap-2">
                        <span>{formatDay(item.effectiveFrom)}</span>
                        <span>
                          {item.ctcAnnual != null ? `${formatMoney(item.ctcAnnual)} CTC` : formatMoney(item.gross)}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              <ErrorBanner error={formError} />
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setDraft(null)}>
                  {t("common.cancel")}
                </Button>
                <Button type="submit" disabled={busy || !preview}>
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
