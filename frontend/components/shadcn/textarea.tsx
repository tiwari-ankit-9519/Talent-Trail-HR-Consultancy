import * as React from "react";
import { cn } from "@/lib/utils";

function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      className={cn(
        "flex min-h-16 w-full rounded-lg border border-line bg-surface px-3 py-2 text-sm text-fg shadow-xs outline-none transition-[color,box-shadow] placeholder:text-muted hover:border-[color-mix(in_oklab,var(--accent)_45%,var(--border))] focus-visible:border-accent focus-visible:ring-[3px] focus-visible:ring-[color-mix(in_oklab,var(--accent)_22%,transparent)] disabled:cursor-not-allowed disabled:opacity-60",
        className,
      )}
      {...props}
    />
  );
}

export { Textarea };
