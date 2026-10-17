"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Briefcase, Eye, EyeOff, FileText, ShieldCheck, Users } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/shadcn/button";
import { Input } from "@/components/shadcn/input";
import ThemeToggle from "@/components/ThemeToggle";
import { ErrorBanner, Field } from "@/components/ui";
import { t, tx } from "@/lib/messages";

const points = [
  { key: "1", icon: Users },
  { key: "2", icon: FileText },
  { key: "3", icon: ShieldCheck },
] as const;

export default function LoginForm() {
  const { user, loading, signIn } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [visible, setVisible] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!loading && user) {
      router.replace(user.mustChangePassword ? "/change-password" : "/dashboard");
    }
  }, [loading, user, router]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const profile = await signIn(email.trim(), password);
      router.replace(profile.mustChangePassword ? "/change-password" : "/dashboard");
    } catch (failure) {
      setError(failure);
      setBusy(false);
    }
  }

  return (
    <main className="login-shell grid min-h-screen bg-bg lg:grid-cols-2">
      <div
        className="relative hidden flex-col justify-between overflow-hidden bg-accent-solid p-12 text-accent-fg lg:flex"
        style={{
          backgroundImage:
            "radial-gradient(36rem 24rem at 0% 0%, rgb(255 255 255 / 0.22), transparent), radial-gradient(30rem 22rem at 100% 100%, rgb(0 0 0 / 0.28), transparent)",
        }}
      >
        <div aria-hidden="true" className="absolute -right-24 top-1/3 size-72 rounded-full bg-white/10" />
        <div aria-hidden="true" className="absolute -left-16 bottom-24 size-48 rounded-full bg-white/10" />
        <div className="relative flex items-center gap-3 text-lg font-semibold">
          <span className="grid size-10 place-items-center rounded-xl bg-white/20 font-bold">T</span>
          {t("app.title")}
        </div>
        <div className="relative grid gap-8">
          <div className="grid gap-3">
            <h2 className="m-0 max-w-md text-4xl font-bold leading-tight tracking-tight text-accent-fg">
              {t("login.panel.heading")}
            </h2>
            <p className="m-0 max-w-md text-base opacity-85">{t("login.panel.text")}</p>
          </div>
          <ul className="m-0 grid list-none gap-4 p-0">
            {points.map(({ key, icon: Icon }) => (
              <li key={key} className="flex items-start gap-3">
                <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-white/20">
                  <Icon aria-hidden="true" className="size-5" />
                </span>
                <span className="grid gap-0.5">
                  <strong>{tx(`login.panel.points.${key}.title`)}</strong>
                  <span className="text-sm opacity-80">{tx(`login.panel.points.${key}.text`)}</span>
                </span>
              </li>
            ))}
          </ul>
        </div>
        <p className="relative m-0 text-sm opacity-70">{t("login.panel.footer")}</p>
      </div>
      <div className="relative flex flex-col justify-center px-6 py-12 sm:px-12">
        <div className="absolute left-6 right-6 top-6 flex items-center justify-between sm:left-12 sm:right-12">
          <Button asChild variant="ghost" size="sm">
            <Link href="/">
              <ArrowLeft aria-hidden="true" className="size-4" />
              {t("login.backHome")}
            </Link>
          </Button>
          <ThemeToggle />
        </div>
        <form className="mx-auto grid w-full max-w-sm gap-5" onSubmit={(e) => void submit(e)}>
          <div className="grid gap-2">
            <div className="flex items-center gap-2 font-semibold text-accent lg:hidden">
              <span className="grid size-8 place-items-center rounded-lg bg-accent-solid text-sm font-bold text-accent-fg">
                T
              </span>
              {t("app.title")}
            </div>
            <h1 className="m-0 text-3xl font-bold tracking-tight">{t("login.heading")}</h1>
            <p className="muted m-0">{t("login.subtitle")}</p>
          </div>
          <ErrorBanner error={error} />
          <Field label={t("login.email")}>
            <Input
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </Field>
          <Field label={t("login.password")}>
            <div className="relative">
              <Input
                type={visible ? "text" : "password"}
                autoComplete="current-password"
                required
                className="pr-11"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="absolute right-1 top-1/2 size-8 -translate-y-1/2"
                aria-label={visible ? t("login.hidePassword") : t("login.showPassword")}
                onClick={() => setVisible((value) => !value)}
              >
                {visible ? (
                  <EyeOff aria-hidden="true" className="size-4" />
                ) : (
                  <Eye aria-hidden="true" className="size-4" />
                )}
              </Button>
            </div>
          </Field>
          <Button type="submit" disabled={busy}>
            {busy ? t("login.submitting") : t("login.submit")}
          </Button>
          <div className="flex items-start gap-3 rounded-xl border border-line bg-surface p-4">
            <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-accent-solid/10 text-accent">
              <Briefcase aria-hidden="true" className="size-4" />
            </span>
            <div className="grid gap-1">
              <strong className="text-sm">{t("login.candidate.title")}</strong>
              <span className="muted text-sm">{t("login.candidate.text")}</span>
              <Link href="/apply" className="text-sm font-medium text-accent">
                {t("login.candidate.action")}
              </Link>
            </div>
          </div>
        </form>
      </div>
    </main>
  );
}
