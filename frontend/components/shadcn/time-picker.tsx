"use client";

import * as React from "react";
import { Clock, X } from "lucide-react";
import { RequiredProxy, triggerClass, type FieldProps } from "@/components/shadcn/date-picker";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/shadcn/popover";
import { cn } from "@/lib/utils";

const hours = Array.from({ length: 12 }, (_, index) => index + 1);
const minutes = Array.from({ length: 60 }, (_, index) => index);

function pad(value: number): string {
  return String(value).padStart(2, "0");
}

function split(value: string): { hour: number; minute: number; pm: boolean } | null {
  const match = /^(\d{2}):(\d{2})/.exec(value);
  if (!match) return null;
  const h24 = Number(match[1]);
  return { hour: h24 % 12 === 0 ? 12 : h24 % 12, minute: Number(match[2]), pm: h24 >= 12 };
}

function join(hour: number, minute: number, pm: boolean): string {
  const h24 = (hour % 12) + (pm ? 12 : 0);
  return `${pad(h24)}:${pad(minute)}`;
}

function Column({
  items,
  selected,
  label,
  onPick,
}: {
  items: { key: string; text: string; value: number }[];
  selected: number | null;
  label: string;
  onPick: (value: number) => void;
}) {
  const ref = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    const active = ref.current?.querySelector<HTMLElement>("[data-active='true']");
    if (active && ref.current) ref.current.scrollTop = active.offsetTop - 72;
  }, []);

  return (
    <div ref={ref} role="listbox" aria-label={label} className="h-52 w-14 overflow-y-auto px-1 py-1">
      {items.map((item) => (
        <button
          key={item.key}
          type="button"
          role="option"
          aria-selected={selected === item.value}
          data-active={selected === item.value}
          className={cn(
            "mb-0.5 block h-8 w-full cursor-pointer rounded-lg border-0 bg-transparent text-sm text-fg transition-colors hover:bg-[color-mix(in_oklab,var(--accent)_14%,transparent)]",
            selected === item.value && "bg-accent-solid font-semibold text-accent-fg shadow-md hover:bg-accent-solid",
          )}
          onClick={() => onPick(item.value)}
        >
          {item.text}
        </button>
      ))}
    </div>
  );
}

function TimePanel({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  const parts = split(value);
  const current = parts ?? { hour: 9, minute: 0, pm: false };

  function emit(next: Partial<typeof current>) {
    const merged = { ...current, ...next };
    onChange(join(merged.hour, merged.minute, merged.pm));
  }

  return (
    <div className="flex gap-1">
      <Column
        label="Hour"
        selected={parts ? parts.hour : null}
        items={hours.map((hour) => ({ key: `h${hour}`, text: pad(hour), value: hour }))}
        onPick={(hour) => emit({ hour })}
      />
      <Column
        label="Minute"
        selected={parts ? parts.minute : null}
        items={minutes.map((minute) => ({ key: `m${minute}`, text: pad(minute), value: minute }))}
        onPick={(minute) => emit({ minute })}
      />
      <Column
        label="AM or PM"
        selected={parts ? (parts.pm ? 1 : 0) : null}
        items={[
          { key: "am", text: "AM", value: 0 },
          { key: "pm", text: "PM", value: 1 },
        ]}
        onPick={(pm) => emit({ pm: pm === 1 })}
      />
    </div>
  );
}

function TimeField({ value = "", onChange, required, disabled, placeholder, className, ...rest }: FieldProps) {
  const [open, setOpen] = React.useState(false);
  const parts = split(value);
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
            <Clock aria-hidden="true" className="size-4 shrink-0 text-muted" />
            <span className={cn("min-w-0 flex-1 truncate", !parts && "text-muted")}>
              {parts
                ? `${pad(parts.hour)}:${pad(parts.minute)} ${parts.pm ? "PM" : "AM"}`
                : (placeholder ?? "Pick a time")}
            </span>
            {!required && parts && !disabled && (
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
        <PopoverContent className="p-1.5">
          <TimePanel value={value} onChange={(next) => onChange?.({ target: { value: next } })} />
        </PopoverContent>
      </Popover>
      <RequiredProxy value={value} required={required} name={rest.name} />
    </div>
  );
}

export { TimeField, TimePanel };
