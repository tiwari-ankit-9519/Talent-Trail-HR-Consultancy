import type { ReactNode } from "react";
import { LandingFooter } from "@/components/landing/ContactFooter";
import LandingHeader from "@/components/landing/LandingHeader";

export default function ApplyLayout({ children }: { children: ReactNode }) {
  return (
    <div className="landing-shell flex min-h-screen flex-col bg-bg">
      <LandingHeader />
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-10">{children}</main>
      <LandingFooter />
    </div>
  );
}
