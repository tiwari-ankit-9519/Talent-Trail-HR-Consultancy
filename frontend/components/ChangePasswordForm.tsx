"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { ErrorBanner, Field, Loading } from "@/components/ui";
import { api, logout } from "@/lib/api";
import { t } from "@/lib/messages";
import { Button } from "@/components/shadcn/button";
import { Input } from "@/components/shadcn/input";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";

export default function ChangePasswordForm() {
  const { user, loading } = useAuth();
  const router = useRouter();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [mismatch, setMismatch] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (next !== confirm) {
      setMismatch(true);
      return;
    }
    setMismatch(false);
    setBusy(true);
    try {
      await api<void>("/auth/change-password", {
        method: "POST",
        body: { currentPassword: current, newPassword: next },
      });
      await logout();
      window.location.assign("/login");
    } catch (failure) {
      setError(failure);
      setBusy(false);
    }
  }

  if (loading || !user) {
    return (
      <main className="center-screen">
        <Loading />
      </main>
    );
  }

  return (
    <main className="center-screen">
      <Card asChild>
        <form className="auth-card" onSubmit={(e) => void submit(e)}>
          <h1>{t("changePassword.heading")}</h1>
          {user.mustChangePassword && <Alert variant="info">{t("changePassword.forced")}</Alert>}
          <p className="muted small">{t("changePassword.policy")}</p>
          <ErrorBanner error={error} />
          {mismatch && <Alert variant="destructive">{t("changePassword.mismatch")}</Alert>}
          <Field label={t("changePassword.current")}>
            <Input
              type="password"
              autoComplete="current-password"
              required
              value={current}
              onChange={(e) => setCurrent(e.target.value)}
            />
          </Field>
          <Field label={t("changePassword.new")}>
            <Input
              type="password"
              autoComplete="new-password"
              required
              value={next}
              onChange={(e) => setNext(e.target.value)}
            />
          </Field>
          <Field label={t("changePassword.confirm")}>
            <Input
              type="password"
              autoComplete="new-password"
              required
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
            />
          </Field>
          <Button type="submit" disabled={busy}>
            {t("changePassword.submit")}
          </Button>
          {!user.mustChangePassword && (
            <Button variant="outline" type="button" onClick={() => router.back()}>
              {t("common.cancel")}
            </Button>
          )}
        </form>
      </Card>
    </main>
  );
}
