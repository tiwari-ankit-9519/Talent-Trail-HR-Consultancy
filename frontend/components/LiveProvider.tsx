"use client";

import { useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { toast } from "sonner";
import { useAuth } from "@/components/AuthProvider";
import { api, openStream } from "@/lib/api";
import { t } from "@/lib/messages";
import type { LivePayload, NotificationFeed, NotificationItem } from "@/lib/types";

type Listener = (payload: LivePayload) => void;

type LiveContextValue = {
  items: NotificationItem[];
  unread: number;
  connected: boolean;
  markRead: (id: string) => Promise<void>;
  markAllRead: () => Promise<void>;
  subscribe: (listener: Listener) => () => void;
};

const LiveContext = createContext<LiveContextValue | null>(null);

function parseBlock(block: string): { event: string; data: string } | null {
  let event = "message";
  const data: string[] = [];
  for (const line of block.split("\n")) {
    if (line.startsWith(":")) continue;
    if (line.startsWith("event:")) event = line.slice(6).trim();
    else if (line.startsWith("data:")) data.push(line.slice(5).trimStart());
  }
  return data.length === 0 ? null : { event, data: data.join("\n") };
}

function wait(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve) => {
    const timer = setTimeout(resolve, ms);
    signal.addEventListener(
      "abort",
      () => {
        clearTimeout(timer);
        resolve();
      },
      { once: true },
    );
  });
}

export function LiveProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const router = useRouter();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unread, setUnread] = useState(0);
  const [connected, setConnected] = useState(false);
  const listeners = useRef(new Set<Listener>());
  const routerRef = useRef(router);
  routerRef.current = router;

  const loadFeed = useCallback(async () => {
    try {
      const feed = await api<NotificationFeed>("/notifications", { query: { limit: 30 } });
      setItems(feed.items);
      setUnread(feed.unread);
    } catch {
      return;
    }
  }, []);

  const emit = useCallback((payload: LivePayload) => {
    listeners.current.forEach((listener) => listener(payload));
  }, []);

  const handle = useCallback(
    (payload: LivePayload) => {
      if (payload.id && payload.title) {
        const item: NotificationItem = {
          id: payload.id,
          topic: payload.topic,
          title: payload.title,
          body: payload.body,
          link: payload.link,
          read: false,
          createdAt: payload.createdAt,
        };
        setItems((current) => [item, ...current.filter((existing) => existing.id !== item.id)].slice(0, 30));
        setUnread((count) => count + 1);
        toast(payload.title, {
          description: payload.body ?? undefined,
          action: payload.link
            ? { label: t("notifications.view"), onClick: () => routerRef.current.push(payload.link as string) }
            : undefined,
        });
      }
      emit(payload);
    },
    [emit],
  );

  useEffect(() => {
    if (!user) return;
    const controller = new AbortController();
    const { signal } = controller;
    let first = true;
    void loadFeed();

    async function run() {
      let delay = 1000;
      while (!signal.aborted) {
        try {
          const response = await openStream("/notifications/stream", signal);
          if (!response.ok || !response.body) throw new Error(String(response.status));
          setConnected(true);
          delay = 1000;
          if (!first) {
            void loadFeed();
            emit({ topic: "resync", id: null, title: null, body: null, link: null, createdAt: "" });
          }
          first = false;
          const reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = "";
          for (;;) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, "\n");
            let index = buffer.indexOf("\n\n");
            while (index >= 0) {
              const parsed = parseBlock(buffer.slice(0, index));
              buffer = buffer.slice(index + 2);
              if (parsed?.event === "live") {
                try {
                  handle(JSON.parse(parsed.data) as LivePayload);
                } catch {
                  continue;
                }
              }
              index = buffer.indexOf("\n\n");
            }
          }
        } catch {
          if (signal.aborted) break;
        }
        setConnected(false);
        if (signal.aborted) break;
        await wait(delay, signal);
        delay = Math.min(delay * 2, 30000);
      }
    }

    void run();
    return () => {
      controller.abort();
      setConnected(false);
    };
  }, [user, loadFeed, handle, emit]);

  const markRead = useCallback(async (id: string) => {
    setItems((current) => current.map((item) => (item.id === id ? { ...item, read: true } : item)));
    setUnread((count) => Math.max(0, count - 1));
    try {
      await api<void>(`/notifications/${id}/read`, { method: "POST" });
    } catch {
      return;
    }
  }, []);

  const markAllRead = useCallback(async () => {
    setItems((current) => current.map((item) => ({ ...item, read: true })));
    setUnread(0);
    try {
      await api<void>("/notifications/read-all", { method: "POST" });
    } catch {
      return;
    }
  }, []);

  const subscribe = useCallback((listener: Listener) => {
    listeners.current.add(listener);
    return () => {
      listeners.current.delete(listener);
    };
  }, []);

  const value = useMemo(
    () => ({ items, unread, connected, markRead, markAllRead, subscribe }),
    [items, unread, connected, markRead, markAllRead, subscribe],
  );

  return <LiveContext.Provider value={value}>{children}</LiveContext.Provider>;
}

export function useNotifications(): LiveContextValue {
  const context = useContext(LiveContext);
  if (!context) throw new Error("useNotifications must be used inside LiveProvider");
  return context;
}

export function useLive(topics: string[], handler: () => void) {
  const context = useContext(LiveContext);
  const handlerRef = useRef(handler);
  handlerRef.current = handler;
  const key = topics.join(",");
  const subscribe = context?.subscribe;

  useEffect(() => {
    if (!subscribe) return;
    const wanted = key.split(",");
    return subscribe((payload) => {
      if (payload.topic === "resync" || wanted.includes(payload.topic)) handlerRef.current();
    });
  }, [subscribe, key]);
}
