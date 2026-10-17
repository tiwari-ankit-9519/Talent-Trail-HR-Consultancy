"use client";

import * as React from "react";
import { ChevronLeft, ChevronRight, CalendarRange } from "lucide-react";
import { Button } from "@/components/shadcn/button";
import { RequiredProxy, triggerClass, type FieldProps } from "@/components/shadcn/date-picker";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/shadcn/popover";
import { cn } from "@/lib/utils";

const names = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const full = [
  "January",
  "February",
  "March",
  "April",
  "May",
  "June",
  "July",
  "August",
  "September",
  "October",
  "November",
  "December",
];

function MonthField({ value = "", onChange, required, disabled, className, ...rest }: FieldProps) {
  const [open, setOpen] = React.useState(false);
  const [year, setYear] = React.useState(() => Number(value.slice(0, 4)) || new Date().getFullYear());
  const selectedYear = Number(value.slice(0, 4));
  const selectedMonth = Number(value.slice(5, 7));
  const label = selectedYear && selectedMonth ? `${full[selectedMonth - 1]} ${selectedYear}` : "";

  return (
    <div className={cn("relative w-full min-w-0", className)}>
      <Popover
        open={open}
        onOpenChange={(next) => {
          setOpen(next);
          if (next) setYear(selectedYear || new Date().getFullYear());
        }}
      >
        <PopoverTrigger asChild>
          <button
            type="button"
            disabled={disabled}
            id={rest.id}
            aria-label={rest["aria-label"]}
            className={triggerClass}
          >
            <CalendarRange aria-hidden="true" className="size-4 shrink-0 text-muted" />
            <span className={cn("min-w-0 flex-1 truncate", !label && "text-muted")}>{label || "Pick a month"}</span>
          </button>
        </PopoverTrigger>
        <PopoverContent className="w-64 p-3">
          <div className="mb-3 flex items-center justify-between">
            <Button type="button" variant="outline" size="icon" className="size-8" onClick={() => setYear(year - 1)}>
              <ChevronLeft aria-hidden="true" className="size-4" />
              <span className="sr-only">Previous year</span>
            </Button>
            <span className="text-sm font-semibold">{year}</span>
            <Button type="button" variant="outline" size="icon" className="size-8" onClick={() => setYear(year + 1)}>
              <ChevronRight aria-hidden="true" className="size-4" />
              <span className="sr-only">Next year</span>
            </Button>
          </div>
          <div className="grid grid-cols-3 gap-1.5">
            {names.map((name, index) => {
              const active = selectedYear === year && selectedMonth === index + 1;
              return (
                <button
                  key={name}
                  type="button"
                  className={cn(
                    "h-9 cursor-pointer rounded-lg border-0 text-sm text-fg transition-colors hover:bg-[color-mix(in_oklab,var(--accent)_14%,transparent)]",
                    active && "bg-accent-solid font-semibold text-accent-fg shadow-md hover:bg-accent-solid",
                  )}
                  onClick={() => {
                    onChange?.({ target: { value: `${year}-${String(index + 1).padStart(2, "0")}` } });
                    setOpen(false);
                  }}
                >
                  {name}
                </button>
              );
            })}
          </div>
        </PopoverContent>
      </Popover>
      <RequiredProxy value={value} required={required} name={rest.name} />
    </div>
  );
}

export { MonthField };
