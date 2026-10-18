"use client";

import {
  Coins,
  CalendarClock,
  FileText,
  Handshake,
  History,
  Send,
  UserRound,
  Wallet,
  type LucideIcon,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t, tx } from "@/lib/messages";
import type { TimelineEvent } from "@/lib/types";

export type TimelineSubject = "PLACEMENT" | "INVOICE" | "CANDIDATE" | "SUBMISSION" | "COMMISSION" | "PAYROLL_RUN";

const icons: Record<string, LucideIcon> = {
  CANDIDATE: UserRound,
  SUBMISSION: Send,
  INTERVIEW: CalendarClock,
  PLACEMENT: Handshake,
  INVOICE: FileText,
  COMMISSION: Coins,
  ENTITY_PAYROLL_RUN: Wallet,
};

const VISIBLE = 8;

function humanize(action: string): string {
  const text = action.toLowerCase().replace(/_/g, " ");
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export default function Timeline({
  subject,
  id,
  refreshKey,
}: {
  subject: TimelineSubject;
  id: string;
  refreshKey?: string | number;
}) {
  const [events, setEvents] = useState<TimelineEvent[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    api<TimelineEvent[]>(`/timeline/${subject}/${id}`)
      .then((result) => {
        if (!cancelled) setEvents(result);
      })
      .catch((failure) => {
        if (!cancelled) setError(failure);
      });
    return () => {
      cancelled = true;
    };
  }, [subject, id, refreshKey]);

  const shown = events ? (expanded ? events : events.slice(0, VISIBLE)) : [];

  return (
    <Card className="grid gap-4 p-5">
      <h2 className="m-0 flex items-center gap-2 text-lg">
        <History aria-hidden="true" className="size-5 text-accent" />
        {t("timeline.title")}
      </h2>
      <ErrorBanner error={error} />
      {!events && !error && <Loading />}
      {events && events.length === 0 && <Empty />}
      {events && events.length > 0 && (
        <ol className="m-0 grid list-none gap-0 p-0">
          {shown.map((event, index) => {
            const Icon = icons[event.entityType ?? ""] ?? History;
            return (
              <li key={event.id} className="relative flex gap-3 pb-5 last:pb-0">
                {index < shown.length - 1 && (
                  <span aria-hidden="true" className="absolute left-[15px] top-8 h-[calc(100%-2rem)] w-px bg-line" />
                )}
                <span className="grid size-8 shrink-0 place-items-center rounded-full bg-accent-solid/10 text-accent">
                  <Icon aria-hidden="true" className="size-4" />
                </span>
                <div className="grid min-w-0 gap-0.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <strong className="text-sm">
                      {tx(`timeline.actions.${event.action}`, humanize(event.action))}
                    </strong>
                    {event.outcome === "FAILURE" && <Badge variant="bad">{t("timeline.failed")}</Badge>}
                  </div>
                  <span className="text-sm">{event.summary}</span>
                  <span className="muted text-xs">
                    {formatDate(event.occurredAt)}
                    {event.actorEmail ? ` ${t("timeline.by")} ${event.actorEmail}` : ""}
                  </span>
                </div>
              </li>
            );
          })}
        </ol>
      )}
      {events && events.length > VISIBLE && (
        <Button type="button" variant="outline" size="sm" className="w-fit" onClick={() => setExpanded((v) => !v)}>
          {expanded ? t("timeline.showLess") : t("timeline.showAll").replace("{count}", String(events.length))}
        </Button>
      )}
    </Card>
  );
}
