import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const alertVariants = cva("relative w-full rounded-lg border px-4 py-3 text-sm [&>p]:m-0 [&_ul]:my-1 [&_ul]:pl-5", {
  variants: {
    variant: {
      default: "border-line bg-surface text-fg",
      destructive:
        "border-[color-mix(in_oklab,var(--bad)_45%,var(--border))] bg-[color-mix(in_oklab,var(--bad)_8%,var(--surface))] text-bad",
      success:
        "border-[color-mix(in_oklab,var(--ok)_45%,var(--border))] bg-[color-mix(in_oklab,var(--ok)_8%,var(--surface))] text-ok",
      info: "border-[color-mix(in_oklab,var(--accent)_45%,var(--border))] bg-[color-mix(in_oklab,var(--accent)_8%,var(--surface))] text-accent",
    },
  },
  defaultVariants: { variant: "default" },
});

function Alert({ className, variant, ...props }: React.ComponentProps<"div"> & VariantProps<typeof alertVariants>) {
  return <div role="alert" className={cn(alertVariants({ variant }), className)} {...props} />;
}

export { Alert };
