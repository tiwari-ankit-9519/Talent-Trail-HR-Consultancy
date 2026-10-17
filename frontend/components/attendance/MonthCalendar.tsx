"use client";

import { ChevronLeft, ChevronRight, Hourglass } from "lucide-react";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { formatMinutes, formatTime, monthLabel, shiftMonth, todayIso, weekdays } from "@/lib/attendance";
import { t, tx } from "@/lib/messages";
import type { AttendanceDay, AttendanceMonth } from "@/lib/types";
import { cn } from "@/lib/utils";

type Tone = { box: string; label: string; dot: string };

const tones: Record<string, Tone> = {
  PRESENT: {
    box: "border-ok/50 bg-[color-mix(in_oklab,var(--ok)_18%,var(--surface))]",
    label: "text-ok",
    dot: "bg-ok",
  },
  HALF_DAY: {
    box: "border-warn/50 bg-[color-mix(in_oklab,var(--warn)_20%,var(--surface))]",
    label: "text-warn",
    dot: "bg-warn",
  },
  ABSENT: {
    box: "border-bad/50 bg-[color-mix(in_oklab,var(--bad)_18%,var(--surface))]",
    label: "text-bad",
    dot: "bg-bad",
  },
  HOLIDAY: {
    box: "border-accent/50 bg-[color-mix(in_oklab,var(--accent)_18%,var(--surface))]",
    label: "text-accent",
    dot: "bg-accent",
  },
  LEAVE: {
    box: "border-[#8b5cf6]/50 bg-[color-mix(in_oklab,#8b5cf6_20%,var(--surface))]",
    label: "text-[#a78bfa]",
    dot: "bg-[#8b5cf6]",
  },
  WEEKLY_OFF: { box: "border-line bg-bg", label: "text-muted", dot: "bg-muted" },
};

const plain: Tone = { box: "border-line bg-surface", label: "text-muted", dot: "bg-muted" };
const legend = ["PRESENT", "HALF_DAY", "ABSENT", "HOLIDAY", "LEAVE", "WEEKLY_OFF"];

function dayNumber(date: string): number {
  return Number(date.slice(8, 10));
}

function Cell({ day, today, onPick }: { day: AttendanceDay; today: string; onPick?: (day: AttendanceDay) => void }) {
  const tone = tones[day.state] ?? plain;
  const pickable = Boolean(onPick) && day.date <= today;
  const hidden = day.state === "UPCOMING" || day.state === "NONE";
  const label = hidden
    ? (day.optionalHolidayName ?? "")
    : (day.holidayName ?? tx(`enums.attendanceState.${day.state}`));
  const content = (
    <>
      <span className="flex items-center justify-between gap-1">
        <span
          className={cn(
            "grid size-6 place-items-center rounded-full text-sm font-semibold text-fg",
            day.date === today && "bg-accent-solid text-accent-fg",
          )}
        >
          {dayNumber(day.date)}
        </span>
        {day.pendingRequest && <Hourglass aria-label={t("attendance.pendingRequest")} className="size-3.5 text-warn" />}
      </span>
      <span className={cn("block truncate text-xs font-medium leading-tight", tone.label)}>{label}</span>
      {day.checkIn && (
        <span className="block truncate text-[0.72rem] leading-tight text-fg">
          {formatTime(day.checkIn)}
          {day.checkOut ? ` – ${formatTime(day.checkOut)}` : ""}
        </span>
      )}
      {(day.late || day.workMode === "WFH") && (
        <span className="flex flex-wrap gap-1">
          {day.late && (
            <span className="rounded bg-warn/25 px-1 text-[0.65rem] font-semibold text-warn">
              {t("attendance.lateShort")}
            </span>
          )}
          {day.workMode === "WFH" && (
            <span className="rounded bg-accent/25 px-1 text-[0.65rem] font-semibold text-accent">
              {t("attendance.wfh")}
            </span>
          )}
        </span>
      )}
    </>
  );
  const className = cn(
    "grid min-h-[5.2rem] min-w-0 content-start gap-1 rounded-lg border p-1.5 text-left text-fg",
    tone.box,
    pickable && "cursor-pointer transition-transform hover:-translate-y-0.5 hover:shadow-md",
  );
  if (pickable) {
    return (
      <button type="button" className={className} onClick={() => onPick?.(day)} title={day.note ?? undefined}>
        {content}
      </button>
    );
  }
  return (
    <div className={className} title={day.note ?? undefined}>
      {content}
    </div>
  );
}

export default function MonthCalendar({
  data,
  monthKey,
  onMonthChange,
  onPick,
  title,
}: {
  data: AttendanceMonth | null;
  monthKey: string;
  onMonthChange: (key: string) => void;
  onPick?: (day: AttendanceDay) => void;
  title?: string;
}) {
  const today = todayIso();
  const lead = data && data.days.length > 0 ? (new Date(`${data.days[0].date}T00:00:00`).getDay() + 6) % 7 : 0;
  const totals = data?.totals;
  const stats: [string, string | number][] = totals
    ? [
        ["present", totals.present],
        ["halfDay", totals.halfDay],
        ["absent", totals.absent],
        ["late", totals.late],
        ["leaves", totals.leaves],
        ["wfh", totals.wfh],
        ["worked", formatMinutes(totals.workedMinutes)],
      ]
    : [];

  return (
    <Card className="grid gap-4 p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="m-0 text-lg">{title ?? t("attendance.month.heading")}</h2>
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="size-8"
            onClick={() => onMonthChange(shiftMonth(monthKey, -1))}
          >
            <ChevronLeft aria-hidden="true" className="size-4" />
            <span className="sr-only">{t("attendance.month.previous")}</span>
          </Button>
          <span className="min-w-36 text-center text-sm font-semibold">{monthLabel(monthKey)}</span>
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="size-8"
            onClick={() => onMonthChange(shiftMonth(monthKey, 1))}
          >
            <ChevronRight aria-hidden="true" className="size-4" />
            <span className="sr-only">{t("attendance.month.next")}</span>
          </Button>
        </div>
      </div>

      {stats.length > 0 && (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
          {stats.map(([key, value]) => (
            <div key={key} className="rounded-lg border border-line bg-bg px-3 py-2">
              <p className="m-0 text-xs text-muted">{tx(`attendance.totals.${key}`)}</p>
              <p className="m-0 text-lg font-semibold text-fg">{value}</p>
            </div>
          ))}
        </div>
      )}

      <div className="overflow-x-auto">
        <div className="grid min-w-[40rem] grid-cols-7 gap-1.5">
          {weekdays.map((name) => (
            <div
              key={name}
              className="px-1 pb-1 text-center text-[0.7rem] font-semibold uppercase tracking-wide text-muted"
            >
              {tx(`attendance.weekdaysShort.${name}`)}
            </div>
          ))}
          {Array.from({ length: lead }).map((_, index) => (
            <div key={`blank-${index}`} aria-hidden="true" />
          ))}
          {data?.days.map((day) => (
            <Cell key={day.date} day={day} today={today} onPick={onPick} />
          ))}
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-muted">
        {legend.map((key) => (
          <span key={key} className="inline-flex items-center gap-1.5">
            <span aria-hidden="true" className={cn("size-2.5 rounded-full", tones[key].dot)} />
            {tx(`enums.attendanceState.${key}`)}
          </span>
        ))}
        {onPick && <span className="ml-auto">{t("attendance.month.pickHint")}</span>}
      </div>
    </Card>
  );
}
