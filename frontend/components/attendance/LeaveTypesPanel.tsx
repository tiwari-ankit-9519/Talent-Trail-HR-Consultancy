"use client";

import { Plus } from "lucide-react";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Checkbox } from "@/components/shadcn/checkbox";
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
import { Empty, ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { confirmDialog } from "@/lib/confirm";
import { t } from "@/lib/messages";
import type { LeaveType } from "@/lib/types";

type Draft = {
  id: string | null;
  name: string;
  yearlyDays: string;
  unpaid: boolean;
  active: boolean;
  version: number | null;
};

const blank: Draft = { id: null, name: "", yearlyDays: "12", unpaid: false, active: true, version: null };

export default function LeaveTypesPanel() {
  const [rows, setRows] = useState<LeaveType[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [formError, setFormError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setRows(await api<LeaveType[]>("/attendance/leave-types", { query: { activeOnly: "false" } }));
    } catch (failure) {
      setError(failure);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!draft) return;
    setBusy(true);
    setFormError(null);
    try {
      const body = {
        name: draft.name,
        yearlyDays: draft.unpaid ? 0 : Number(draft.yearlyDays),
        unpaid: draft.unpaid,
        active: draft.active,
        version: draft.version,
      };
      if (draft.id) {
        await api<LeaveType>(`/attendance/leave-types/${draft.id}`, { method: "PUT", body });
      } else {
        await api<LeaveType>("/attendance/leave-types", { method: "POST", body });
      }
      setDraft(null);
      await load();
    } catch (failure) {
      setFormError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function remove(row: LeaveType) {
    if (!(await confirmDialog(t("attendance.types.deleteConfirm").replace("{name}", row.name)))) return;
    setError(null);
    try {
      await api<void>(`/attendance/leave-types/${row.id}`, { method: "DELETE" });
      await load();
    } catch (failure) {
      setError(failure);
    }
  }

  return (
    <div className="grid gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="muted m-0 max-w-2xl text-sm">{t("attendance.types.help")}</p>
        <Button
          type="button"
          onClick={() => {
            setFormError(null);
            setDraft(blank);
          }}
        >
          <Plus aria-hidden="true" className="size-4" />
          {t("attendance.types.add")}
        </Button>
      </div>
      <ErrorBanner error={error} />
      {!rows && !error && <Loading />}
      {rows && rows.length === 0 && <Empty />}
      {rows && rows.length > 0 && (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("attendance.types.name")}</TableHead>
                <TableHead>{t("attendance.types.yearly")}</TableHead>
                <TableHead>{t("attendance.types.kind")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("common.actions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{row.name}</TableCell>
                  <TableCell>{row.unpaid ? "-" : row.yearlyDays}</TableCell>
                  <TableCell>
                    <Badge variant={row.unpaid ? "warn" : "accent"}>
                      {row.unpaid ? t("attendance.types.unpaid") : t("attendance.types.paid")}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge variant={row.active ? "ok" : "default"}>
                      {row.active ? t("common.active") : t("common.inactive")}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setFormError(null);
                          setDraft({
                            id: row.id,
                            name: row.name,
                            yearlyDays: String(row.yearlyDays),
                            unpaid: row.unpaid,
                            active: row.active,
                            version: row.version,
                          });
                        }}
                      >
                        {t("common.edit")}
                      </Button>
                      <Button type="button" size="sm" variant="outline" onClick={() => void remove(row)}>
                        {t("common.delete")}
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog open={draft !== null} onOpenChange={(open) => !open && setDraft(null)}>
        <DialogContent closeLabel={t("common.close")}>
          <DialogHeader>
            <DialogTitle>{draft?.id ? t("attendance.types.edit") : t("attendance.types.add")}</DialogTitle>
            <DialogDescription>{t("attendance.types.dialogHelp")}</DialogDescription>
          </DialogHeader>
          {draft && (
            <form className="grid gap-3" onSubmit={(event) => void save(event)}>
              <Field label={t("attendance.types.name")}>
                <Input
                  required
                  maxLength={60}
                  value={draft.name}
                  onChange={(e) => setDraft({ ...draft, name: e.target.value })}
                />
              </Field>
              <label className="flex cursor-pointer items-center gap-2 text-sm">
                <Checkbox
                  checked={draft.unpaid}
                  onCheckedChange={(checked) => setDraft({ ...draft, unpaid: checked === true })}
                />
                {t("attendance.types.unpaidLabel")}
              </label>
              {!draft.unpaid && (
                <Field label={t("attendance.types.yearly")} hint={t("attendance.types.yearlyHint")}>
                  <Input
                    required
                    type="number"
                    min={0}
                    max={365}
                    step={0.5}
                    value={draft.yearlyDays}
                    onChange={(e) => setDraft({ ...draft, yearlyDays: e.target.value })}
                  />
                </Field>
              )}
              <label className="flex cursor-pointer items-center gap-2 text-sm">
                <Checkbox
                  checked={draft.active}
                  onCheckedChange={(checked) => setDraft({ ...draft, active: checked === true })}
                />
                {t("attendance.types.activeLabel")}
              </label>
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
