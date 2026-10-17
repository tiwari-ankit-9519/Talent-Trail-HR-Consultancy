"use client";

import { CalendarHeart, Gift } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { Input } from "@/components/shadcn/input";
import StatusPill from "@/components/StatusPill";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { todayIso } from "@/lib/attendance";
import { confirmDialog } from "@/lib/confirm";
import { formatDay, t, tx } from "@/lib/messages";
import type { HolidayEntry, LeaveApplication, LeaveBalance, LeaveOverview } from "@/lib/types";
import { DateField } from "@/components/shadcn/date-picker";
import LeaveApplyDialog from "@/components/attendance/LeaveApplyDialog";
import { applicationRange } from "@/components/attendance/LeaveApplicationsPanel";

export default function LeavePanel({ refreshKey, onChanged }: { refreshKey: number; onChanged: () => void }) {
  const [data, setData] = useState<LeaveOverview | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [balances, setBalances] = useState<LeaveBalance[]>([]);
  const [applications, setApplications] = useState<LeaveApplication[]>([]);
  const [applyOpen, setApplyOpen] = useState(false);
  const [compDate, setCompDate] = useState(todayIso());
  const [busy, setBusy] = useState(false);
  const today = todayIso();

  const load = useCallback(async () => {
    try {
      const [overview, balanceRows, mine] = await Promise.all([
        api<LeaveOverview>("/attendance/leave/overview"),
        api<LeaveBalance[]>("/attendance/leave/balances"),
        api<LeaveApplication[]>("/attendance/leave/applications/mine"),
      ]);
      setData(overview);
      setBalances(balanceRows);
      setApplications(mine);
    } catch (failure) {
      setError(failure);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load, refreshKey]);

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      await load();
      onChanged();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function cancelApplication(id: string) {
    if (!(await confirmDialog(t("attendance.leave.cancelConfirm")))) return;
    await run(() => api<void>(`/attendance/leave/applications/${id}`, { method: "DELETE" }));
  }

  async function cancel(id: string) {
    if (!(await confirmDialog(t("attendance.leave.cancelConfirm")))) return;
    await run(() => api<void>(`/attendance/leave/${id}`, { method: "DELETE" }));
  }

  const upcoming: HolidayEntry[] = (data?.holidays ?? []).filter((holiday) => holiday.date >= today);
  const quotaLeft = data ? data.optionalQuota - data.optionalUsed : 0;
  const nextCredit = data?.compOffs.find((credit) => credit.status === "AVAILABLE");

  return (
    <Card className="grid content-start gap-4 p-5">
      <h2 className="m-0 flex items-center gap-2 text-lg">
        <CalendarHeart aria-hidden="true" className="size-5 text-accent" />
        {t("attendance.leave.heading")}
      </h2>
      <ErrorBanner error={error} />
      {data && (
        <>
          <div className="grid gap-2">
            <div className="flex items-center justify-between gap-2">
              <h3 className="m-0 text-sm font-semibold">{t("attendance.leave.balances")}</h3>
              <Button type="button" size="sm" onClick={() => setApplyOpen(true)}>
                {t("attendance.apply.open")}
              </Button>
            </div>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              {balances.map((balance) => (
                <div key={balance.leaveTypeId} className="rounded-lg border border-line bg-bg px-3 py-2">
                  <p className="m-0 truncate text-xs text-muted">{balance.name}</p>
                  {balance.unpaid ? (
                    <p className="m-0 text-lg font-semibold text-fg">
                      {balance.used}
                      <span className="text-sm font-normal text-muted"> {t("attendance.leave.taken")}</span>
                    </p>
                  ) : (
                    <p className="m-0 text-lg font-semibold text-fg">
                      {balance.remaining}
                      <span className="text-sm font-normal text-muted"> / {balance.yearlyDays}</span>
                    </p>
                  )}
                </div>
              ))}
            </div>
          </div>

          {applications.filter((a) => a.status === "PENDING" || (a.status === "APPROVED" && a.toDate >= today)).length >
            0 && (
            <div className="grid gap-2">
              <h3 className="m-0 text-sm font-semibold">{t("attendance.leave.myApplications")}</h3>
              <ul className="m-0 grid list-none gap-1.5 p-0">
                {applications
                  .filter((a) => a.status === "PENDING" || (a.status === "APPROVED" && a.toDate >= today))
                  .map((a) => (
                    <li
                      key={a.id}
                      className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-bg px-3 py-2"
                    >
                      <span className="min-w-0 text-sm text-fg">
                        <span className="block font-medium">
                          {a.typeName} <StatusPill kind="requestStatus" value={a.status} />
                        </span>
                        <span className="text-xs text-muted">
                          {applicationRange(a)} · {a.days} {t("attendance.apply.daysUnit")}
                        </span>
                      </span>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        disabled={busy}
                        onClick={() => void cancelApplication(a.id)}
                      >
                        {t("common.cancel")}
                      </Button>
                    </li>
                  ))}
              </ul>
            </div>
          )}

          <div className="grid grid-cols-2 gap-2">
            <div className="rounded-lg border border-line bg-bg px-3 py-2">
              <p className="m-0 text-xs text-muted">{t("attendance.leave.optionalBalance")}</p>
              <p className="m-0 text-lg font-semibold text-fg">
                {quotaLeft}
                <span className="text-sm font-normal text-muted"> / {data.optionalQuota}</span>
              </p>
            </div>
            <div className="rounded-lg border border-line bg-bg px-3 py-2">
              <p className="m-0 text-xs text-muted">{t("attendance.leave.compOffBalance")}</p>
              <p className="m-0 text-lg font-semibold text-fg">{data.compOffAvailable}</p>
              {nextCredit && (
                <p className="m-0 text-[0.7rem] text-muted">
                  {t("attendance.leave.expires").replace("{date}", formatDay(nextCredit.expiresOn))}
                </p>
              )}
            </div>
          </div>

          <div className="grid gap-2">
            <h3 className="m-0 text-sm font-semibold">{t("attendance.leave.upcomingHolidays")}</h3>
            {upcoming.length === 0 ? (
              <p className="muted small m-0">{t("attendance.leave.noHolidays")}</p>
            ) : (
              <ul className="m-0 grid list-none gap-1.5 p-0">
                {upcoming.slice(0, 8).map((holiday) => (
                  <li
                    key={holiday.id}
                    className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-bg px-3 py-2"
                  >
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-medium text-fg">{holiday.name}</span>
                      <span className="text-xs text-muted">{formatDay(holiday.date)}</span>
                    </span>
                    <span className="flex items-center gap-2">
                      <StatusPill kind="holidayKind" value={holiday.kind} />
                      {holiday.claimed && holiday.leaveStatus && (
                        <StatusPill kind="requestStatus" value={holiday.leaveStatus} />
                      )}
                      {holiday.kind === "OPTIONAL" &&
                        (holiday.claimed ? (
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            disabled={busy}
                            onClick={() => holiday.leaveId && void cancel(holiday.leaveId)}
                          >
                            {t("attendance.leave.release")}
                          </Button>
                        ) : (
                          <Button
                            type="button"
                            size="sm"
                            disabled={busy || quotaLeft <= 0}
                            onClick={() =>
                              void run(() =>
                                api<void>("/attendance/leave/optional", {
                                  method: "POST",
                                  body: { holidayId: holiday.id },
                                }),
                              )
                            }
                          >
                            {t("attendance.leave.request")}
                          </Button>
                        ))}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="grid gap-2">
            <h3 className="m-0 flex items-center gap-2 text-sm font-semibold">
              <Gift aria-hidden="true" className="size-4 text-accent" />
              {t("attendance.leave.useCompOff")}
            </h3>
            <div className="flex flex-wrap items-end gap-2">
              <Field label={t("attendance.leave.compOffDate")}>
                <DateField required min={today} value={compDate} onChange={(e) => setCompDate(e.target.value)} />
              </Field>
              <Button
                type="button"
                disabled={busy || data.compOffAvailable === 0 || !compDate}
                onClick={() =>
                  void run(() => api<void>("/attendance/leave/comp-off", { method: "POST", body: { date: compDate } }))
                }
              >
                {t("attendance.leave.takeCompOff")}
              </Button>
            </div>
            <p className="muted small m-0">{t("attendance.leave.compOffHelp")}</p>
          </div>

          {data.leaves.filter((leave) => leave.date >= today).length > 0 && (
            <div className="grid gap-2">
              <h3 className="m-0 text-sm font-semibold">{t("attendance.leave.myLeaves")}</h3>
              <ul className="m-0 grid list-none gap-1.5 p-0">
                {data.leaves
                  .filter((leave) => leave.date >= today)
                  .map((leave) => (
                    <li
                      key={leave.id}
                      className="flex items-center justify-between gap-2 rounded-lg border border-line bg-bg px-3 py-2"
                    >
                      <span className="text-sm text-fg">
                        {formatDay(leave.date)} <Badge variant="accent">{tx(`enums.leaveKind.${leave.kind}`)}</Badge>{" "}
                        <StatusPill kind="requestStatus" value={leave.status} />
                      </span>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        disabled={busy}
                        onClick={() => void cancel(leave.id)}
                      >
                        {t("common.cancel")}
                      </Button>
                    </li>
                  ))}
              </ul>
            </div>
          )}
          <LeaveApplyDialog
            open={applyOpen}
            onOpenChange={setApplyOpen}
            balances={balances}
            onApplied={() => {
              void load();
              onChanged();
            }}
          />
        </>
      )}
    </Card>
  );
}
