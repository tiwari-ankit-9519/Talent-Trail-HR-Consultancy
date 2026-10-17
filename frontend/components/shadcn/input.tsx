import * as React from "react";
import { cn } from "@/lib/utils";

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      className={cn(
        "flex h-9 w-full min-w-0 rounded-lg border border-line bg-surface px-3 py-1 text-sm text-fg shadow-xs outline-none transition-[color,box-shadow] placeholder:text-muted file:mr-3 file:inline-flex file:h-7 file:cursor-pointer file:items-center file:rounded-md file:border-0 file:bg-bg file:px-3 file:text-sm file:font-medium file:text-fg hover:border-[color-mix(in_oklab,var(--accent)_45%,var(--border))] focus-visible:border-accent focus-visible:ring-[3px] focus-visible:ring-[color-mix(in_oklab,var(--accent)_22%,transparent)] disabled:cursor-not-allowed disabled:opacity-60",
        className,
      )}
      {...props}
    />
  );
}

export { Input };
