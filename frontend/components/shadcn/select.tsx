"use client";

import * as React from "react";
import * as SelectPrimitive from "@radix-ui/react-select";
import { Check, ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

const Select = SelectPrimitive.Root;
const SelectValue = SelectPrimitive.Value;

function SelectTrigger({ className, children, ...props }: React.ComponentProps<typeof SelectPrimitive.Trigger>) {
  return (
    <SelectPrimitive.Trigger
      className={cn(
        "flex h-9 w-full min-w-0 cursor-pointer items-center justify-between gap-2 rounded-lg border border-line bg-surface px-3 text-left text-sm text-fg shadow-xs outline-none transition-[color,box-shadow] hover:border-[color-mix(in_oklab,var(--accent)_45%,var(--border))] focus-visible:border-accent focus-visible:ring-[3px] focus-visible:ring-[color-mix(in_oklab,var(--accent)_22%,transparent)] disabled:cursor-not-allowed disabled:opacity-60 data-[placeholder]:text-muted [&>span]:truncate",
        className,
      )}
      {...props}
    >
      {children}
      <SelectPrimitive.Icon asChild>
        <ChevronDown aria-hidden="true" className="size-4 shrink-0 text-muted" />
      </SelectPrimitive.Icon>
    </SelectPrimitive.Trigger>
  );
}

function SelectContent({
  className,
  children,
  position = "popper",
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Content>) {
  return (
    <SelectPrimitive.Portal>
      <SelectPrimitive.Content
        position={position}
        sideOffset={4}
        className={cn(
          "animate-fade-in relative z-[80] max-h-72 min-w-[8rem] overflow-hidden rounded-lg border border-line bg-surface text-fg shadow-lg",
          position === "popper" && "w-[var(--radix-select-trigger-width)]",
          className,
        )}
        {...props}
      >
        <SelectPrimitive.Viewport className="p-1">{children}</SelectPrimitive.Viewport>
      </SelectPrimitive.Content>
    </SelectPrimitive.Portal>
  );
}

function SelectItem({ className, children, ...props }: React.ComponentProps<typeof SelectPrimitive.Item>) {
  return (
    <SelectPrimitive.Item
      className={cn(
        "relative flex w-full cursor-pointer select-none items-center rounded-md py-1.5 pl-2 pr-8 text-sm outline-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 data-[highlighted]:bg-[color-mix(in_oklab,var(--accent)_12%,transparent)] data-[highlighted]:text-accent",
        className,
      )}
      {...props}
    >
      <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
      <span className="absolute right-2 flex size-4 items-center justify-center">
        <SelectPrimitive.ItemIndicator>
          <Check aria-hidden="true" className="size-4" />
        </SelectPrimitive.ItemIndicator>
      </span>
    </SelectPrimitive.Item>
  );
}

const EMPTY = "__empty__";

type OptionData = { value: string; label: React.ReactNode; disabled: boolean };

function collect(children: React.ReactNode, into: OptionData[]) {
  React.Children.forEach(children, (child) => {
    if (!React.isValidElement(child)) return;
    const props = child.props as { value?: string | number; children?: React.ReactNode; disabled?: boolean };
    if (child.type === React.Fragment) {
      collect(props.children, into);
      return;
    }
    if (child.type === "option") {
      const label = props.children;
      const value = props.value !== undefined ? String(props.value) : String(label ?? "");
      into.push({ value, label, disabled: !!props.disabled });
    }
  });
}

type ChangeLike = { target: { value: string } };

type FormSelectProps = {
  value?: string;
  onChange?: (event: ChangeLike) => void;
  children?: React.ReactNode;
  required?: boolean;
  disabled?: boolean;
  name?: string;
  id?: string;
  className?: string;
  placeholder?: string;
  "aria-label"?: string;
};

function FormSelect({ value = "", onChange, children, placeholder, className, ...rest }: FormSelectProps) {
  const options: OptionData[] = [];
  collect(children, options);
  const hasEmpty = options.some((option) => option.value === "");
  const current = value === "" ? (hasEmpty ? EMPTY : undefined) : value;
  return (
    <Select
      value={current}
      onValueChange={(next) => onChange?.({ target: { value: next === EMPTY ? "" : next } })}
      required={rest.required}
      disabled={rest.disabled}
      name={rest.name}
    >
      <SelectTrigger id={rest.id} aria-label={rest["aria-label"]} className={className}>
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {options.map((option) => (
          <SelectItem
            key={option.value || EMPTY}
            value={option.value === "" ? EMPTY : option.value}
            disabled={option.disabled}
          >
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

export { Select, SelectTrigger, SelectValue, SelectContent, SelectItem, FormSelect };
