import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { Card } from "@/components/shadcn/card";
import { cn } from "@/lib/utils";

type Tone = "accent" | "ok" | "warn" | "bad";

const toneClass: Record<Tone, string> = {
  accent: "bg-[color-mix(in_oklab,var(--accent)_14%,transparent)] text-accent",
  ok: "bg-[color-mix(in_oklab,var(--ok)_14%,transparent)] text-ok",
  warn: "bg-[color-mix(in_oklab,var(--warn)_16%,transparent)] text-warn",
  bad: "bg-[color-mix(in_oklab,var(--bad)_14%,transparent)] text-bad",
};

type Props = {
  label: string;
  value: string;
  hint?: string;
  title?: string;
  icon: LucideIcon;
  tone?: Tone;
  href?: string;
};

export default function StatCard({ label, value, hint, title, icon: Icon, tone = "accent", href }: Props) {
  const body = (
    <Card className={cn("h-full p-3.5 transition-shadow sm:p-4", href && "hover:shadow-md")}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="m-0 text-sm leading-snug text-muted">{label}</p>
          <p className="m-0 mt-1 truncate text-xl font-semibold tracking-tight sm:text-2xl" title={title}>
            {value}
          </p>
        </div>
        <span className={cn("grid size-8 shrink-0 place-items-center rounded-xl sm:size-10", toneClass[tone])}>
          <Icon aria-hidden="true" className="size-4 sm:size-5" />
        </span>
      </div>
      {hint && <p className="m-0 mt-2 text-xs text-muted">{hint}</p>}
    </Card>
  );
  return href ? (
    <Link
      href={href}
      className="block rounded-card no-underline text-fg focus-visible:outline-2 focus-visible:outline-accent"
    >
      {body}
    </Link>
  ) : (
    body
  );
}
