"use client";

import Link from "next/link";
import { ChevronDown, Menu } from "lucide-react";
import { useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { industries, services } from "@/components/landing/content";
import { Button } from "@/components/shadcn/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/shadcn/dropdown-menu";
import { Sheet, SheetClose, SheetContent, SheetDescription, SheetTitle, SheetTrigger } from "@/components/shadcn/sheet";
import ThemeToggle from "@/components/ThemeToggle";
import { t, tx } from "@/lib/messages";

const links = [
  { href: "/#about", label: "landing.nav.about" },
  { href: "/#industries", label: "landing.nav.industries" },
  { href: "/#process", label: "landing.nav.process" },
  { href: "/#contact", label: "landing.nav.contact" },
] as const;

export default function LandingHeader() {
  const { user, loading } = useAuth();
  const [open, setOpen] = useState(false);
  const signedIn = !loading && user !== null;

  return (
    <header className="sticky top-0 z-30 border-b border-line bg-surface/85 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4">
        <Link href="/" className="flex items-center gap-2 font-semibold text-fg no-underline">
          <span className="grid size-8 place-items-center rounded-lg bg-accent-solid text-sm font-bold text-accent-fg">
            T
          </span>
          <span>{t("app.title")}</span>
        </Link>
        <nav aria-label={t("landing.nav.label")} className="hidden items-center gap-1 lg:flex">
          <Button asChild variant="ghost" size="sm">
            <Link href="/">{t("landing.nav.home")}</Link>
          </Button>
          <Button asChild variant="ghost" size="sm">
            <Link href="/#about">{t("landing.nav.about")}</Link>
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button type="button" variant="ghost" size="sm">
                {t("landing.nav.services")}
                <ChevronDown aria-hidden="true" className="size-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-60">
              <DropdownMenuLabel>{t("landing.nav.byService")}</DropdownMenuLabel>
              {services.map((service) => (
                <DropdownMenuItem key={service.key} asChild>
                  <Link href="/#services">{tx(`landing.services.${service.key}.title`)}</Link>
                </DropdownMenuItem>
              ))}
              <DropdownMenuSeparator />
              <DropdownMenuLabel>{t("landing.nav.byIndustry")}</DropdownMenuLabel>
              {industries.map((industry) => (
                <DropdownMenuItem key={industry.key} asChild>
                  <Link href="/#industries">{tx(`landing.industries.${industry.key}`)}</Link>
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
          {links.slice(1).map((link) => (
            <Button key={link.href} asChild variant="ghost" size="sm">
              <Link href={link.href}>{tx(link.label)}</Link>
            </Button>
          ))}
        </nav>
        <div className="flex items-center gap-2">
          <ThemeToggle />
          <Button asChild variant="outline" size="sm" className="hidden sm:inline-flex">
            <Link href="/apply">{t("landing.nav.jobs")}</Link>
          </Button>
          <Button asChild size="sm" className="hidden sm:inline-flex">
            <Link href={signedIn ? "/dashboard" : "/login"}>
              {signedIn ? t("landing.nav.dashboard") : t("landing.nav.signIn")}
            </Link>
          </Button>
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild>
              <Button
                type="button"
                variant="outline"
                size="icon"
                className="lg:hidden"
                aria-label={t("landing.nav.menu")}
              >
                <Menu aria-hidden="true" className="size-5" />
              </Button>
            </SheetTrigger>
            <SheetContent className="left-auto right-0 border-l border-r-0">
              <SheetTitle className="px-4 pt-4">{t("app.title")}</SheetTitle>
              <SheetDescription className="sr-only">{t("landing.nav.label")}</SheetDescription>
              <div className="grid gap-1 p-4">
                <SheetClose asChild>
                  <Button asChild variant="ghost" className="justify-start">
                    <Link href="/">{t("landing.nav.home")}</Link>
                  </Button>
                </SheetClose>
                <SheetClose asChild>
                  <Button asChild variant="ghost" className="justify-start">
                    <Link href="/#about">{t("landing.nav.about")}</Link>
                  </Button>
                </SheetClose>
                <SheetClose asChild>
                  <Button asChild variant="ghost" className="justify-start">
                    <Link href="/#services">{t("landing.nav.services")}</Link>
                  </Button>
                </SheetClose>
                {links.slice(1).map((link) => (
                  <SheetClose key={link.href} asChild>
                    <Button asChild variant="ghost" className="justify-start">
                      <Link href={link.href}>{tx(link.label)}</Link>
                    </Button>
                  </SheetClose>
                ))}
                <SheetClose asChild>
                  <Button asChild variant="outline" className="mt-2">
                    <Link href="/apply">{t("landing.nav.jobs")}</Link>
                  </Button>
                </SheetClose>
                <SheetClose asChild>
                  <Button asChild>
                    <Link href={signedIn ? "/dashboard" : "/login"}>
                      {signedIn ? t("landing.nav.dashboard") : t("landing.nav.signIn")}
                    </Link>
                  </Button>
                </SheetClose>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}
