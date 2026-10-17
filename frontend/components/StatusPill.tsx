import { Badge } from "@/components/shadcn/badge";
import { tx } from "@/lib/messages";
import { cn } from "@/lib/utils";

type Tone = "default" | "accent" | "ok" | "warn" | "bad";

const shared: Record<string, Tone> = {
  COMPLETED: "ok",
  PAID: "ok",
  SENT: "ok",
  GRANTED: "ok",
  JOINED: "ok",
  OFFER_ACCEPTED: "ok",
  SELECTED: "ok",
  AVAILABLE: "ok",
  FILLED: "ok",
  PAYABLE: "ok",
  PLACED: "ok",
  PRESENT: "ok",
  APPROVED: "ok",
  HALF_DAY: "warn",
  HOLIDAY: "accent",
  LEAVE: "accent",
  USED: "default",
  EXPIRED: "bad",
  ABSENT: "bad",
  OPEN: "ok",
  QUEUED: "accent",
  ISSUED: "accent",
  SCHEDULED: "accent",
  SUBMITTED: "accent",
  SHORTLISTED: "accent",
  INTERVIEWING: "accent",
  NEW: "accent",
  OFFERED: "accent",
  SCREENING: "accent",
  PENDING: "warn",
  ON_HOLD: "warn",
  PARTIALLY_PAID: "warn",
  NOT_CONFIGURED: "warn",
  CANCELLED: "bad",
  REJECTED: "bad",
  FAILED: "bad",
  DENIED: "bad",
  WITHDRAWN: "bad",
  NO_SHOW: "bad",
  OFFER_DECLINED: "bad",
  NOT_JOINED: "bad",
  BLOCKED: "bad",
  NOT_INTERESTED: "bad",
  NOT_ELIGIBLE: "bad",
};

const overrides: Record<string, Record<string, Tone>> = {
  replacementStatus: { OPEN: "warn", CLOSED: "default" },
  jobStatus: { OPEN: "ok", CLOSED: "default" },
  emailStatus: { SKIPPED: "default" },
  holidayKind: { NATIONAL: "accent", OPTIONAL: "warn" },
  workMode: { OFFICE: "default", WFH: "accent" },
};

const dots: Record<Tone, string> = {
  default: "bg-muted",
  accent: "bg-accent",
  ok: "bg-ok",
  warn: "bg-warn",
  bad: "bg-bad",
};

export default function StatusPill({
  kind,
  value,
  className,
}: {
  kind: string;
  value: string | null | undefined;
  className?: string;
}) {
  if (!value) return null;
  const tone = overrides[kind]?.[value] ?? shared[value] ?? "default";
  return (
    <Badge variant={tone} className={cn("gap-1.5", className)}>
      <span
        aria-hidden="true"
        className={cn("size-1.5 rounded-full", dots[tone], value === "QUEUED" && "animate-pulse")}
      />
      {tx(`enums.${kind}.${value}`, value)}
    </Badge>
  );
}
