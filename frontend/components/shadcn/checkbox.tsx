"use client";

import * as React from "react";
import * as CheckboxPrimitive from "@radix-ui/react-checkbox";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

function Checkbox({ className, ...props }: React.ComponentProps<typeof CheckboxPrimitive.Root>) {
  return (
    <CheckboxPrimitive.Root
      className={cn(
        "peer relative flex size-[1.125rem] shrink-0 cursor-pointer items-center justify-center rounded-[5px] border border-line bg-surface text-accent-fg shadow-xs outline-none transition-colors hover:border-accent focus-visible:ring-[3px] focus-visible:ring-[color-mix(in_oklab,var(--accent)_25%,transparent)] disabled:cursor-not-allowed disabled:opacity-60 data-[state=checked]:border-accent-solid data-[state=checked]:bg-accent-solid",
        className,
      )}
      {...props}
    >
      <CheckboxPrimitive.Indicator className="flex items-center justify-center">
        <Check aria-hidden="true" className="size-3 text-accent-fg" strokeWidth={3.5} />
      </CheckboxPrimitive.Indicator>
    </CheckboxPrimitive.Root>
  );
}

type CheckboxFieldProps = {
  checked?: boolean;
  onChange?: (event: { target: { checked: boolean } }) => void;
  disabled?: boolean;
  required?: boolean;
  id?: string;
  name?: string;
  className?: string;
  "aria-label"?: string;
};

function CheckboxField({ checked = false, onChange, ...rest }: CheckboxFieldProps) {
  return (
    <Checkbox
      checked={checked}
      onCheckedChange={(next) => onChange?.({ target: { checked: next === true } })}
      {...rest}
    />
  );
}

export { Checkbox, CheckboxField };
