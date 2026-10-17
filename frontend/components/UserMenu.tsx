"use client";

import { ChevronDown, KeyRound, LogOut } from "lucide-react";
import Link from "next/link";
import { useAuth } from "@/components/AuthProvider";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/shadcn/dropdown-menu";
import { t } from "@/lib/messages";

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const letters = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : (parts[0] ?? "?").slice(0, 2);
  return letters.toUpperCase();
}

export default function UserMenu() {
  const { user, signOut } = useAuth();
  if (!user) return null;
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="flex cursor-pointer items-center gap-2 rounded-full border border-line bg-surface py-1 pl-1 pr-2.5 text-fg shadow-xs outline-none transition-all hover:border-accent focus-visible:ring-[3px] focus-visible:ring-[color-mix(in_oklab,var(--accent)_25%,transparent)]"
        >
          <span
            aria-hidden="true"
            className="grid size-7 place-items-center rounded-full bg-gradient-to-br from-accent-solid to-[var(--accent-2)] text-[0.7rem] font-semibold text-accent-fg"
          >
            {initials(user.fullName)}
          </span>
          <span className="hidden max-w-32 truncate text-sm font-medium sm:block">{user.fullName}</span>
          <ChevronDown aria-hidden="true" className="size-3.5 text-muted" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        <DropdownMenuLabel>
          <span className="block truncate text-sm font-semibold text-fg">{user.fullName}</span>
          <span className="block truncate text-xs font-normal text-muted">{user.email}</span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link href="/change-password" className="text-fg no-underline">
            <KeyRound aria-hidden="true" className="size-4 text-muted" />
            {t("nav.changePassword")}
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={() => void signOut()}>
          <LogOut aria-hidden="true" className="size-4 text-muted" />
          {t("nav.signOut")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
