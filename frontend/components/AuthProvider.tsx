"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import {
  login as apiLogin,
  logout as apiLogout,
  refreshSession,
  setAccessToken,
  setSessionLostHandler,
} from "@/lib/api";
import type { UserProfile } from "@/lib/types";

type AuthContextValue = {
  user: UserProfile | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<UserProfile>;
  signOut: () => Promise<void>;
  applySession: (accessToken: string, user: UserProfile) => void;
  can: (...permissions: string[]) => boolean;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void refreshSession().then((session) => {
      if (!active) return;
      setUser(session ? session.user : null);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    setSessionLostHandler(() => {
      setUser(null);
      router.replace("/login");
    });
    return () => setSessionLostHandler(null);
  }, [router]);

  const signIn = useCallback(async (email: string, password: string) => {
    const session = await apiLogin(email, password);
    setUser(session.user);
    return session.user;
  }, []);

  const signOut = useCallback(async () => {
    await apiLogout();
    setUser(null);
    router.replace("/login");
  }, [router]);

  const applySession = useCallback((accessToken: string, next: UserProfile) => {
    setAccessToken(accessToken);
    setUser(next);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      signIn,
      signOut,
      applySession,
      can: (...permissions: string[]) => !!user && permissions.some((p) => user.permissions.includes(p)),
    }),
    [user, loading, signIn, signOut, applySession],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
