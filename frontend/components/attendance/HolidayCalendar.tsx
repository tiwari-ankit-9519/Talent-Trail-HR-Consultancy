"use client";

import { CalendarDays, ChevronLeft, ChevronRight } from "lucide-react";
import { useEffect, useState } from "react";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { todayIso } from "@/lib/attendance";
import { t, tx } from "@/lib/messages";
import type { AttendanceHoliday } from "@/lib/types";
import { cn } from "@/lib/utils";

const months = Array.from({ length: 12 }, (_, index) => index);

function monthName(year: number, month: number): string {
  return new Date(year, month, 1).toLocaleDateString(undefined, { month: "long" });
}

function dayParts(date: string): { day: number; weekday: string } {
  const [year, month, day] = date.split("-").map(Number);
  return { day, weekday: new Date(year, month - 1, day).toLocaleDateString(undefined, { weekday: "short" }) };
}

export default function HolidayCalendar() {
  const [year, setYear] = useState(Number(todayIso().slice(0, 4)));
  const [kind, setKind] = useState("ALL");
  const [rows, setRows] = useState<AttendanceHoliday[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const today = todayIso();

  useEffect(() => {
    setRows(null);
    setError(null);
    api<AttendanceHoliday[]>("/attendance/holidays", { query: { year: String(year) } })
      .then(setRows)
      .catch(setError);
  }, [year]);

  const visible = (rows ?? []).filter((holiday) => kind === "ALL" || holiday.kind === kind);
  const national = (rows ?? []).filter((holiday) => holiday.kind === "NATIONAL").length;
  const optional = (rows ?? []).filter((holiday) => holiday.kind === "OPTIONAL").length;

  return (
    <Card className="grid gap-4 p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="m-0 flex items-center gap-2 text-lg">
          <CalendarDays aria-hidden="true" className="size-5 text-accent" />
          {t("attendance.holidayCalendar.heading")}
        </h2>
        <div className="flex items-center gap-2">
          <Button type="button" variant="outline" size="icon" className="size-8" onClick={() => setYear(year - 1)}>
            <ChevronLeft aria-hidden="true" className="size-4" />
            <span className="sr-only">{t("attendance.month.previous")}</span>
          </Button>
          <span className="min-w-16 text-center text-sm font-semibold">{year}</span>
          <Button type="button" variant="outline" size="icon" className="size-8" onClick={() => setYear(year + 1)}>
            <ChevronRight aria-hidden="true" className="size-4" />
            <span className="sr-only">{t("attendance.month.next")}</span>
          </Button>
        </div>
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Tabs value={kind} onValueChange={setKind}>
          <TabsList>
            <TabsTrigger value="ALL">{t("attendance.holidayCalendar.all")}</TabsTrigger>
            <TabsTrigger value="NATIONAL">{tx("enums.holidayKind.NATIONAL")}</TabsTrigger>
            <TabsTrigger value="OPTIONAL">{tx("enums.holidayKind.OPTIONAL")}</TabsTrigger>
          </TabsList>
        </Tabs>
        {rows && (
          <p className="muted m-0 text-sm">
            {t("attendance.holidayCalendar.counts")
              .replace("{national}", String(national))
              .replace("{optional}", String(optional))}
          </p>
        )}
      </div>
      <ErrorBanner error={error} />
      {!rows && !error && <Loading />}
      {rows && visible.length === 0 && <p className="muted m-0 text-sm">{t("attendance.holidayCalendar.none")}</p>}
      {rows && visible.length > 0 && (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {months.map((month) => {
            const list = visible.filter((holiday) => Number(holiday.date.slice(5, 7)) === month + 1);
            if (list.length === 0) return null;
            return (
              <div key={month} className="grid min-w-0 content-start gap-2 rounded-lg border border-line bg-bg p-3">
                <h3 className="m-0 text-sm font-semibold">{monthName(year, month)}</h3>
                <ul className="m-0 grid min-w-0 list-none gap-1.5 p-0">
                  {list.map((holiday) => {
                    const parts = dayParts(holiday.date);
                    return (
                      <li
                        key={holiday.id}
                        className={cn(
                          "flex min-w-0 items-center gap-3 rounded-md border border-line bg-surface px-2.5 py-2",
                          holiday.date === today && "border-accent",
                        )}
                      >
                        <span className="grid w-10 shrink-0 justify-items-center">
                          <span className="text-lg font-semibold leading-none text-fg">{parts.day}</span>
                          <span className="text-[0.65rem] uppercase text-muted">{parts.weekday}</span>
                        </span>
                        <span className="min-w-0 flex-1 truncate text-sm text-fg">{holiday.name}</span>
                        <Badge className="shrink-0" variant={holiday.kind === "NATIONAL" ? "accent" : "warn"}>
                          {tx(`enums.holidayKind.${holiday.kind}`)}
                        </Badge>
                      </li>
                    );
                  })}
                </ul>
              </div>
            );
          })}
        </div>
      )}
    </Card>
  );
}
