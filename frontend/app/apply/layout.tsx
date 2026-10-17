import Link from "next/link";
import type { ReactNode } from "react";
import { t } from "@/lib/messages";

export default function ApplyLayout({ children }: { children: ReactNode }) {
  return (
    <div className="public-shell">
      <header className="public-header">
        <Link href="/apply" className="brand">
          {t("app.title")}
        </Link>
        <span className="muted">{t("apply.tagline")}</span>
      </header>
      <main className="public-content">{children}</main>
    </div>
  );
}
