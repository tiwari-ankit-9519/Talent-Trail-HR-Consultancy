"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { ErrorBanner, Field } from "@/components/ui";
import { t } from "@/lib/messages";
import { Button } from "@/components/shadcn/button";
import { Input } from "@/components/shadcn/input";
import { Card } from "@/components/shadcn/card";

export default function LoginForm() {
  const { user, loading, signIn } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
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
    <main className="center-screen">
      <Card asChild>
        <form className="auth-card" onSubmit={(e) => void submit(e)}>
          <p className="brand">{t("app.title")}</p>
          <h1>{t("login.heading")}</h1>
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
            <Input
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </Field>
          <Button type="submit" disabled={busy}>
            {busy ? t("login.submitting") : t("login.submit")}
          </Button>
        </form>
      </Card>
    </main>
  );
}
