"use client";

import { Bell, CheckCheck } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useNotifications } from "@/components/LiveProvider";
import { Button } from "@/components/shadcn/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/shadcn/popover";
import { t } from "@/lib/messages";
import type { NotificationItem } from "@/lib/types";
import { cn } from "@/lib/utils";

const units: [Intl.RelativeTimeFormatUnit, number][] = [
  ["day", 86400],
  ["hour", 3600],
  ["minute", 60],
];

function ago(iso: string): string {
  const seconds = Math.round((new Date(iso).getTime() - Date.now()) / 1000);
  const formatter = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });
  for (const [unit, size] of units) {
    if (Math.abs(seconds) >= size) return formatter.format(Math.round(seconds / size), unit);
  }
  return formatter.format(0, "second");
}

export default function NotificationBell() {
  const { items, unread, connected, markRead, markAllRead } = useNotifications();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  function pick(item: NotificationItem) {
    if (!item.read) void markRead(item.id);
    setOpen(false);
    if (item.link) router.push(item.link);
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button variant="outline" size="icon" className="relative" aria-label={t("notifications.label")}>
          <Bell aria-hidden="true" className="size-4" />
          {unread > 0 && (
            <span className="absolute -right-1.5 -top-1.5 grid min-w-5 place-items-center rounded-full bg-[#c0392b] px-1 text-[0.65rem] font-semibold leading-5 text-white">
              {unread > 9 ? "9+" : unread}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[22rem] max-w-[calc(100vw-2rem)]">
        <div className="flex items-center justify-between gap-2 border-b border-line px-4 py-3">
          <div className="flex items-center gap-2">
            <h2 className="m-0 text-sm font-semibold">{t("notifications.heading")}</h2>
            <span
              title={connected ? t("notifications.live") : t("notifications.offline")}
              className={cn("size-2 rounded-full", connected ? "bg-ok" : "bg-muted")}
            />
          </div>
          <Button type="button" variant="ghost" size="sm" disabled={unread === 0} onClick={() => void markAllRead()}>
            <CheckCheck aria-hidden="true" className="size-4" />
            {t("notifications.markAll")}
          </Button>
        </div>
        <div className="max-h-96 overflow-y-auto">
          {items.length === 0 ? (
            <p className="muted m-0 px-4 py-6 text-center text-sm">{t("notifications.empty")}</p>
          ) : (
            <ul className="m-0 grid list-none p-0">
              {items.map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    onClick={() => pick(item)}
                    className="flex w-full cursor-pointer gap-3 border-0 border-b border-solid border-line bg-transparent px-4 py-3 text-left text-fg outline-none transition-colors last:border-b-0 hover:bg-bg focus-visible:bg-bg"
                  >
                    <span
                      aria-hidden="true"
                      className={cn("mt-1.5 size-2 shrink-0 rounded-full", item.read ? "bg-transparent" : "bg-accent")}
                    />
                    <span className="min-w-0 flex-1">
                      <span className={cn("block text-sm text-fg", !item.read && "font-semibold")}>{item.title}</span>
                      {item.body && <span className="block text-xs text-muted">{item.body}</span>}
                      <span className="block text-[0.7rem] text-muted">{ago(item.createdAt)}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </PopoverContent>
    </Popover>
  );
}
