import type { Metadata } from "next";
import type { ReactNode } from "react";
import { ThemeProvider } from "@/components/ThemeProvider";
import ThemeToggle from "@/components/ThemeToggle";
import { AuthProvider } from "@/components/AuthProvider";
import ConfirmHost from "@/components/ConfirmHost";
import { t } from "@/lib/messages";
import { themeScript } from "@/lib/theme-script";
import "./globals.css";
import "./tailwind.css";

export const metadata: Metadata = {
  title: t("app.title"),
  description: t("app.tagline"),
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript() }} />
      </head>
      <body>
        <ThemeProvider>
          <div className="floating-theme">
            <ThemeToggle />
          </div>
          <AuthProvider>{children}</AuthProvider>
          <ConfirmHost />
        </ThemeProvider>
      </body>
    </html>
  );
}
