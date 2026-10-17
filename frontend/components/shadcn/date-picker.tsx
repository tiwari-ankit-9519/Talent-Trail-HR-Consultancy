"use client";

import * as React from "react";
import { format } from "date-fns";
import { CalendarDays, X } from "lucide-react";
import { Calendar } from "@/components/shadcn/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/shadcn/popover";
import { cn } from "@/lib/utils";

type ChangeEvent = { target: { value: string } };

type FieldProps = {
  value?: string;
  onChange?: (event: ChangeEvent) => void;
  min?: string;
  max?: string;
  required?: boolean;
  disabled?: boolean;
  placeholder?: string;
  id?: string;
  name?: string;
  className?: string;
  "aria-label"?: string;
};

const triggerClass =
  "flex h-9 w-full min-w-0 items-center gap-2 rounded-lg border border-line bg-surface px-3 text-left text-sm text-fg shadow-xs outline-none transition-[color,box-shadow,border-color] hover:border-[color-mix(in_oklab,var(--accent)_45%,var(--border))] focus-visible:border-accent focus-visible:ring-[3px] focus-visible:ring-[color-mix(in_oklab,var(--accent)_22%,transparent)] disabled:cursor-not-allowed disabled:opacity-60 data-[state=open]:border-accent data-[state=open]:ring-[3px] data-[state=open]:ring-[color-mix(in_oklab,var(--accent)_22%,transparent)]";

export function parseDay(value: string | undefined): Date | undefined {
  if (!value) return undefined;
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) return undefined;
  return new Date(year, month - 1, day);
}

export function toDay(date: Date): string {
  return format(date, "yyyy-MM-dd");
}

export function RequiredProxy({ value, required, name }: { value: string; required?: boolean; name?: string }) {
  if (!required && !name) return null;
  return (
    <input
      tabIndex={-1}
      aria-hidden="true"
      required={required}
      name={name}
      value={value}
      onChange={() => {}}
      className="pointer-events-none absolute inset-x-0 bottom-0 h-px w-full opacity-0"
    />
  );
}

function DateField({
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
  const selected = parseDay(value);
  const minDate = parseDay(min);
  const maxDate = parseDay(max);
  const matchers = [...(minDate ? [{ before: minDate }] : []), ...(maxDate ? [{ after: maxDate }] : [])];

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
            <CalendarDays aria-hidden="true" className="size-4 shrink-0 text-muted" />
            <span className={cn("min-w-0 flex-1 truncate", !selected && "text-muted")}>
              {selected ? format(selected, "dd MMM yyyy") : (placeholder ?? "Pick a date")}
            </span>
            {!required && selected && !disabled && (
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
        <PopoverContent>
          <Calendar
            mode="single"
            selected={selected}
            defaultMonth={selected ?? maxDate ?? new Date()}
            disabled={matchers.length > 0 ? matchers : undefined}
            onSelect={(date) => {
              if (date) {
                onChange?.({ target: { value: toDay(date) } });
                setOpen(false);
              }
            }}
          />
        </PopoverContent>
      </Popover>
      <RequiredProxy value={value} required={required} name={rest.name} />
    </div>
  );
}

export { DateField, triggerClass };
export type { FieldProps, ChangeEvent };
