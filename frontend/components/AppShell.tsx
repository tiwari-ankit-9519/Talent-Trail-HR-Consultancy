"use client";

import { Menu } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { useAuth } from "@/components/AuthProvider";
import Breadcrumbs from "@/components/Breadcrumbs";
import ThemeToggle from "@/components/ThemeToggle";
import UserMenu from "@/components/UserMenu";
import SidebarNav from "@/components/SidebarNav";
import { Button } from "@/components/shadcn/button";
import { Sheet, SheetContent, SheetDescription, SheetTitle, SheetTrigger } from "@/components/shadcn/sheet";
import { Loading } from "@/components/ui";
import { BreadcrumbProvider } from "@/lib/breadcrumb";
import { t } from "@/lib/messages";

export default function AppShell({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (loading) return;
    if (!user) router.replace("/login");
    else if (user.mustChangePassword) router.replace("/change-password");
  }, [loading, user, router]);

  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  if (loading || !user || user.mustChangePassword) {
    return (
      <main className="center-screen">
        <Loading />
      </main>
    );
  }

  return (
    <BreadcrumbProvider>
      <div className="app-shell app-bg min-h-screen lg:grid lg:grid-cols-[16.5rem_1fr]">
        <aside className="sticky top-0 hidden h-screen border-r border-line bg-surface/80 backdrop-blur-xl lg:block">
          <SidebarNav />
        </aside>

        <div className="min-w-0">
          <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-line bg-surface/70 px-4 backdrop-blur-xl sm:px-6 lg:px-8">
            <Sheet open={open} onOpenChange={setOpen}>
              <SheetTrigger asChild>
                <Button variant="outline" size="icon" aria-label={t("nav.openMenu")} className="lg:hidden">
                  <Menu aria-hidden="true" className="size-5" />
                </Button>
              </SheetTrigger>
              <SheetContent>
                <SheetTitle className="sr-only">{t("nav.label")}</SheetTitle>
                <SheetDescription className="sr-only">{t("nav.menuDescription")}</SheetDescription>
                <SidebarNav onNavigate={() => setOpen(false)} />
              </SheetContent>
            </Sheet>
            <div className="min-w-0 flex-1">
              <Breadcrumbs className="m-0" />
            </div>
            <ThemeToggle />
            <UserMenu />
          </header>
          <main className="content mx-auto w-full max-w-[90rem]">
            <div key={pathname} className="page-enter">
              {children}
            </div>
          </main>
        </div>
      </div>
    </BreadcrumbProvider>
  );
}
