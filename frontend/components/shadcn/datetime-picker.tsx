"use client";

import * as React from "react";
import { format } from "date-fns";
import { CalendarClock, X } from "lucide-react";
import { Calendar } from "@/components/shadcn/calendar";
import { RequiredProxy, parseDay, toDay, triggerClass, type FieldProps } from "@/components/shadcn/date-picker";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/shadcn/popover";
import { TimePanel } from "@/components/shadcn/time-picker";
import { cn } from "@/lib/utils";

function DateTimeField({
  value = "",
  onChange,
  min,
  max,
  required,
  disabled,
  placeholder,
  className,
  ...rest
}: FieldProps) {
  const [open, setOpen] = React.useState(false);
  const [datePart, timePart] = value.split("T");
  const selected = parseDay(datePart);
  const time = timePart ? timePart.slice(0, 5) : "";
  const minDate = parseDay(min?.slice(0, 10));
  const maxDate = parseDay(max?.slice(0, 10));
  const matchers = [...(minDate ? [{ before: minDate }] : []), ...(maxDate ? [{ after: maxDate }] : [])];

  function emit(day: string, clock: string) {
    if (!day) {
      onChange?.({ target: { value: "" } });
      return;
    }
    onChange?.({ target: { value: `${day}T${clock || "09:00"}` } });
  }

  const label = selected
    ? `${format(selected, "dd MMM yyyy")}${time ? `, ${format(new Date(`2000-01-01T${time}`), "hh:mm a")}` : ""}`
    : "";

  return (
    <div className={cn("relative w-full min-w-0", className)}>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <button
            type="button"
            disabled={disabled}
            id={rest.id}
            aria-label={rest["aria-label"]}
            className={triggerClass}
          >
            <CalendarClock aria-hidden="true" className="size-4 shrink-0 text-muted" />
            <span className={cn("min-w-0 flex-1 truncate", !label && "text-muted")}>
              {label || (placeholder ?? "Pick date and time")}
            </span>
            {!required && label && !disabled && (
              <span
                role="button"
                tabIndex={0}
                aria-label="Clear"
                className="grid size-5 place-items-center rounded-md text-muted transition-colors hover:bg-bg hover:text-fg"
                onClick={(event) => {
                  event.stopPropagation();
                  event.preventDefault();
                  onChange?.({ target: { value: "" } });
                }}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.stopPropagation();
                    event.preventDefault();
                    onChange?.({ target: { value: "" } });
                  }
                }}
              >
                <X aria-hidden="true" className="size-3.5" />
              </span>
            )}
          </button>
        </PopoverTrigger>
        <PopoverContent className="flex flex-col sm:flex-row">
          <Calendar
            mode="single"
            selected={selected}
            defaultMonth={selected ?? new Date()}
            disabled={matchers.length > 0 ? matchers : undefined}
            onSelect={(date) => date && emit(toDay(date), time)}
          />
          <div className="border-t border-line p-1.5 sm:border-l sm:border-t-0">
            <TimePanel value={time} onChange={(next) => emit(datePart || toDay(new Date()), next)} />
          </div>
        </PopoverContent>
      </Popover>
      <RequiredProxy value={value} required={required} name={rest.name} />
    </div>
  );
}

export { DateTimeField };
