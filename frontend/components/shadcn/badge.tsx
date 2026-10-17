import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium whitespace-nowrap",
  {
    variants: {
      variant: {
        default: "border-line bg-bg text-muted",
        accent: "border-transparent bg-[color-mix(in_oklab,var(--accent)_14%,transparent)] text-accent",
        ok: "border-transparent bg-[color-mix(in_oklab,var(--ok)_14%,transparent)] text-ok",
        warn: "border-transparent bg-[color-mix(in_oklab,var(--warn)_16%,transparent)] text-warn",
        bad: "border-transparent bg-[color-mix(in_oklab,var(--bad)_14%,transparent)] text-bad",
      },
    },
    defaultVariants: { variant: "default" },
  },
);

function Badge({ className, variant, ...props }: React.ComponentProps<"span"> & VariantProps<typeof badgeVariants>) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
