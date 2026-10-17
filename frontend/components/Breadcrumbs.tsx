"use client";

import { ChevronRight, Home } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useBreadcrumbLabelValue } from "@/lib/breadcrumb";
import { tx } from "@/lib/messages";

export default function Breadcrumbs({ className = "mb-4" }: { className?: string }) {
  const pathname = usePathname();
  const dynamicLabel = useBreadcrumbLabelValue();
  const segments = pathname.split("/").filter(Boolean);
  if (segments.length === 0 || segments[0] === "dashboard") return null;

  const crumbs = segments.map((segment, index) => {
    const href = "/" + segments.slice(0, index + 1).join("/");
    const known = tx(`breadcrumbs.${segment}`, "");
    const isLast = index === segments.length - 1;
    const label = known || (isLast && dynamicLabel ? dynamicLabel : tx("breadcrumbs.details", "Details"));
    return { href, label, isLast };
  });

  return (
    <nav aria-label={tx("breadcrumbs.label", "Breadcrumb")} className={className}>
      <ol className="m-0 flex list-none flex-wrap items-center gap-1 p-0 text-sm text-muted">
        <li className="flex items-center">
          <Link
            href="/dashboard"
            className="inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-muted no-underline transition-colors hover:bg-[color-mix(in_oklab,var(--accent)_10%,transparent)] hover:text-accent"
          >
            <Home aria-hidden="true" className="size-3.5" />
            <span className="sr-only">{tx("nav.dashboard", "Dashboard")}</span>
          </Link>
        </li>
        {crumbs.map((crumb) => (
          <li key={crumb.href} className="flex min-w-0 items-center gap-1">
            <ChevronRight aria-hidden="true" className="size-3.5 shrink-0 opacity-60" />
            {crumb.isLast ? (
              <span aria-current="page" className="max-w-[16rem] truncate rounded-md px-1.5 py-0.5 font-medium text-fg">
                {crumb.label}
              </span>
            ) : (
              <Link
                href={crumb.href}
                className="rounded-md px-1.5 py-0.5 text-muted no-underline transition-colors hover:bg-[color-mix(in_oklab,var(--accent)_10%,transparent)] hover:text-accent"
              >
                {crumb.label}
              </Link>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
}
