import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex shrink-0 cursor-pointer items-center border border-transparent justify-center gap-2 rounded-lg text-sm font-medium transition-all duration-150 active:scale-[0.97] outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default:
          "bg-gradient-to-b from-[color-mix(in_oklab,var(--accent-solid)_92%,white)] to-accent-solid text-accent-fg shadow-[0_6px_16px_-6px_var(--accent-solid)] hover:-translate-y-px hover:brightness-110 hover:shadow-[0_10px_22px_-6px_var(--accent-solid)] hover:shadow-md",
        ghost: "bg-transparent text-fg hover:bg-bg",
        outline:
          "border-line bg-surface text-fg shadow-xs hover:border-accent hover:bg-[color-mix(in_oklab,var(--accent)_7%,var(--surface))]",
        link: "bg-transparent text-accent underline underline-offset-4 hover:opacity-80",
        destructive: "bg-[#c0392b] text-white hover:brightness-110",
      },
      size: {
        default: "h-9 px-3",
        sm: "h-8 px-2.5 text-xs",
        icon: "size-9",
      },
    },
    defaultVariants: { variant: "default", size: "default" },
  },
);

function Button({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: React.ComponentProps<"button"> & VariantProps<typeof buttonVariants> & { asChild?: boolean }) {
  const Comp = asChild ? Slot : "button";
  return <Comp className={cn(buttonVariants({ variant, size }), className)} {...props} />;
}

export { Button, buttonVariants };
