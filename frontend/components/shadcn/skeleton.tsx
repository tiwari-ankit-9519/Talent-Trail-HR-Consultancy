import * as React from "react";
import { cn } from "@/lib/utils";

function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      className={cn("animate-pulse rounded-md bg-[color-mix(in_oklab,var(--muted)_22%,transparent)]", className)}
      {...props}
    />
  );
}

export { Skeleton };
