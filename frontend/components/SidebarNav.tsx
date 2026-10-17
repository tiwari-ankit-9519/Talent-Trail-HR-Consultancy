"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BarChart3,
  Briefcase,
  Building2,
  CalendarCheck,
  CalendarClock,
  ClipboardCheck,
  Coins,
  FileText,
  Handshake,
  Activity,
  Landmark,
  ReceiptText,
  LayoutDashboard,
  Repeat2,
  ScrollText,
  Send,
  Settings,
  UserCheck,
  ShieldCheck,
  UserCog,
  Users,
  type LucideIcon,
} from "lucide-react";
import { useAuth } from "@/components/AuthProvider";
import { t, type MessageKey } from "@/lib/messages";
import { cn } from "@/lib/utils";

type NavItem = {
  href: string;
  label: MessageKey;
  icon: LucideIcon;
  permissions: string[];
  exact?: boolean;
};

type NavGroup = {
  label: MessageKey | null;
  items: NavItem[];
};

const groups: NavGroup[] = [
  {
    label: null,
    items: [{ href: "/dashboard", label: "nav.dashboard", icon: LayoutDashboard, permissions: [] }],
  },
  {
    label: "nav.groups.recruitment",
    items: [
      { href: "/clients", label: "nav.clients", icon: Building2, permissions: ["CLIENTS_VIEW", "CLIENTS_MANAGE"] },
      { href: "/jobs", label: "nav.jobs", icon: Briefcase, permissions: ["JOBS_VIEW", "JOBS_MANAGE"] },
      {
        href: "/candidates",
        label: "nav.candidates",
        icon: Users,
        permissions: ["CANDIDATES_VIEW", "CANDIDATES_MANAGE"],
      },
      {
        href: "/submissions",
        label: "nav.submissions",
        icon: Send,
        permissions: ["SUBMISSIONS_VIEW", "SUBMISSIONS_MANAGE"],
      },
      {
        href: "/interviews",
        label: "nav.interviews",
        icon: CalendarClock,
        permissions: ["SUBMISSIONS_VIEW", "SUBMISSIONS_MANAGE"],
      },
      {
        href: "/placements",
        label: "nav.placements",
        icon: Handshake,
        permissions: ["PLACEMENTS_VIEW", "PLACEMENTS_MANAGE"],
      },
      {
        href: "/replacements",
        label: "nav.replacements",
        icon: Repeat2,
        permissions: ["REPLACEMENTS_VIEW", "REPLACEMENTS_MANAGE"],
      },
    ],
  },
  {
    label: "nav.groups.finance",
    items: [
      { href: "/invoices", label: "nav.invoices", icon: FileText, permissions: ["INVOICES_VIEW", "INVOICES_MANAGE"] },
      { href: "/reports", label: "nav.reports", icon: BarChart3, permissions: ["REPORTS_VIEW"] },
      {
        href: "/commissions",
        label: "nav.commissions",
        icon: Coins,
        permissions: ["COMMISSIONS_VIEW", "COMMISSIONS_VIEW_OWN", "COMMISSIONS_MANAGE"],
      },
      { href: "/payroll", label: "nav.payroll", icon: Landmark, permissions: ["PAYROLL_MANAGE"] },
    ],
  },
  {
    label: "nav.groups.people",
    items: [
      {
        href: "/attendance",
        label: "nav.attendance",
        icon: CalendarCheck,
        permissions: ["ATTENDANCE_SELF", "ATTENDANCE_MANAGE"],
        exact: true,
      },
      {
        href: "/attendance/leave-requests",
        label: "nav.leaveApprovals",
        icon: UserCheck,
        permissions: ["ATTENDANCE_APPROVE", "ATTENDANCE_MANAGE"],
      },
      {
        href: "/attendance/team",
        label: "nav.teamAttendance",
        icon: ClipboardCheck,
        permissions: ["ATTENDANCE_MANAGE"],
      },
      { href: "/payslips", label: "nav.payslips", icon: ReceiptText, permissions: ["PAYROLL_SELF", "PAYROLL_MANAGE"] },
    ],
  },
  {
    label: "nav.groups.administration",
    items: [
      { href: "/users", label: "nav.users", icon: UserCog, permissions: ["USERS_VIEW", "USERS_MANAGE"] },
      { href: "/roles", label: "nav.roles", icon: ShieldCheck, permissions: ["ROLES_VIEW", "ROLES_MANAGE"] },
      { href: "/settings", label: "nav.settings", icon: Settings, permissions: ["SETTINGS_MANAGE"] },
      { href: "/audit", label: "nav.audit", icon: ScrollText, permissions: ["AUDIT_VIEW"] },
      { href: "/system", label: "nav.system", icon: Activity, permissions: ["SYSTEM_VIEW"] },
    ],
  },
];

export default function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  const { user, can } = useAuth();
  const pathname = usePathname();

  if (!user) return null;

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex items-center gap-3 px-5 pb-4 pt-5">
        <span
          aria-hidden="true"
          className="grid size-9 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-accent-solid to-accent-2 text-sm font-bold text-white shadow-[0_8px_18px_-6px_var(--accent-solid)]"
        >
          T
        </span>
        <span className="min-w-0 leading-tight">
          <span className="block truncate text-sm font-semibold">{t("app.title")}</span>
          <span className="block truncate text-xs text-muted">{t("nav.tagline")}</span>
        </span>
      </div>

      <nav aria-label={t("nav.label")} className="min-h-0 flex-1 space-y-5 overflow-y-auto px-3 pb-4">
        {groups.map((group) => {
          const visible = group.items.filter((item) => item.permissions.length === 0 || can(...item.permissions));
          if (visible.length === 0) return null;
          return (
            <div key={group.label ?? "root"} className="space-y-1">
              {group.label && (
                <p className="m-0 px-3 pb-1 text-[0.68rem] font-semibold uppercase tracking-wider text-muted">
                  {t(group.label)}
                </p>
              )}
              {visible.map((item) => {
                const active = item.exact
                  ? pathname === item.href
                  : pathname === item.href || pathname.startsWith(`${item.href}/`);
                const Icon = item.icon;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={onNavigate}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium no-underline transition-colors",
                      active
                        ? "bg-gradient-to-r from-[color-mix(in_oklab,var(--accent-solid)_22%,transparent)] to-[color-mix(in_oklab,var(--accent-solid)_4%,transparent)] font-semibold text-accent"
                        : "text-muted hover:translate-x-0.5 hover:bg-[color-mix(in_oklab,var(--accent)_8%,transparent)] hover:text-fg",
                    )}
                  >
                    {active && (
                      <span
                        aria-hidden="true"
                        className="absolute inset-y-1.5 left-0 w-1 rounded-r-full bg-gradient-to-b from-accent-solid to-accent-2"
                      />
                    )}
                    <Icon aria-hidden="true" className="size-[1.1rem] shrink-0" />
                    <span className="truncate">{t(item.label)}</span>
                  </Link>
                );
              })}
            </div>
          );
        })}
      </nav>
    </div>
  );
}
