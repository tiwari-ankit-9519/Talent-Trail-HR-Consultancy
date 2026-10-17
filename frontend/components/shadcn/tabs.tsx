"use client";

import * as React from "react";
import * as TabsPrimitive from "@radix-ui/react-tabs";
import { cn } from "@/lib/utils";

const Tabs = TabsPrimitive.Root;

function TabsList({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.List>) {
  return (
    <TabsPrimitive.List
      className={cn(
        "mb-5 inline-flex max-w-full items-center gap-1 overflow-x-auto rounded-xl border border-line bg-surface/80 p-1 shadow-[var(--shadow-card)] backdrop-blur [scrollbar-width:none]",
        className,
      )}
      {...props}
    />
  );
}

function TabsTrigger({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Trigger>) {
  return (
    <TabsPrimitive.Trigger
      className={cn(
        "inline-flex h-8 cursor-pointer items-center justify-center whitespace-nowrap rounded-lg border border-transparent bg-transparent px-4 text-sm font-medium text-muted outline-none transition-all duration-200 hover:bg-[color-mix(in_oklab,var(--accent)_8%,transparent)] hover:text-fg focus-visible:ring-[3px] focus-visible:ring-[color-mix(in_oklab,var(--accent)_25%,transparent)] data-[state=active]:bg-gradient-to-b data-[state=active]:from-[color-mix(in_oklab,var(--accent-solid)_92%,white)] data-[state=active]:to-accent-solid data-[state=active]:text-accent-fg data-[state=active]:shadow-[0_6px_14px_-6px_var(--accent-solid)]",
        className,
      )}
      {...props}
    />
  );
}

const TabsContent = TabsPrimitive.Content;

export { Tabs, TabsList, TabsTrigger, TabsContent };
