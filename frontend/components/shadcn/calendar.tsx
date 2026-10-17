"use client";

import * as React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { DayPicker, type DayPickerProps } from "react-day-picker";
import { buttonVariants } from "@/components/shadcn/button";
import { cn } from "@/lib/utils";

function Calendar({ className, classNames, showOutsideDays = true, ...props }: DayPickerProps) {
  return (
    <DayPicker
      showOutsideDays={showOutsideDays}
      weekStartsOn={1}
      className={cn("p-3", className)}
      classNames={{
        root: "w-fit",
        months: "relative flex flex-col gap-4",
        month: "grid gap-3",
        month_caption: "flex h-8 items-center justify-center",
        caption_label: "text-sm font-semibold",
        nav: "absolute inset-x-0 top-0 z-10 flex items-center justify-between",
        button_previous: cn(buttonVariants({ variant: "outline", size: "icon" }), "size-8"),
        button_next: cn(buttonVariants({ variant: "outline", size: "icon" }), "size-8"),
        month_grid: "w-full border-collapse",
        weekdays: "flex",
        weekday: "w-9 pb-1 text-center text-[0.7rem] font-semibold uppercase text-muted",
        week: "mt-1 flex",
        day: "relative size-9 p-0 text-center text-sm",
        day_button:
          "size-9 cursor-pointer rounded-lg border-0 bg-transparent text-sm font-normal text-fg transition-colors hover:bg-[color-mix(in_oklab,var(--accent)_14%,transparent)] focus-visible:outline-2 focus-visible:outline-accent",
        selected:
          "[&>button]:bg-accent-solid [&>button]:font-semibold [&>button]:text-accent-fg [&>button]:shadow-md [&>button:hover]:bg-accent-solid",
        today: "[&>button]:ring-1 [&>button]:ring-accent [&>button]:font-semibold",
        outside: "[&>button]:text-muted [&>button]:opacity-50",
        disabled: "[&>button]:pointer-events-none [&>button]:opacity-30",
        hidden: "invisible",
        ...classNames,
      }}
      components={{
        Chevron: ({ orientation }) =>
          orientation === "left" ? (
            <ChevronLeft aria-hidden="true" className="size-4" />
          ) : (
            <ChevronRight aria-hidden="true" className="size-4" />
          ),
      }}
      {...props}
    />
  );
}

export { Calendar };
