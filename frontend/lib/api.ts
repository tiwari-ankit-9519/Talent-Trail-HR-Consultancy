import type { Session } from "@/lib/types";

const base = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api") + "/v1";

export class ApiError extends Error {
  status: number;
  code: string;
  fields: Record<string, string>;

  constructor(status: number, code: string, message: string, fields: Record<string, string>) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields;
  }
}

let accessToken: string | null = null;
let refreshing: Promise<Session | null> | null = null;
let onSessionLost: (() => void) | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function setSessionLostHandler(handler: (() => void) | null) {
  onSessionLost = handler;
}

async function toError(response: Response): Promise<ApiError> {
  let body: Record<string, unknown> = {};
  try {
    body = (await response.json()) as Record<string, unknown>;
  } catch {
    body = {};
  }
  const fields = body.fields && typeof body.fields === "object" ? (body.fields as Record<string, string>) : {};
  return new ApiError(
    response.status,
    typeof body.code === "string" ? body.code : "UNKNOWN",
    typeof body.detail === "string" ? body.detail : response.statusText,
    fields,
  );
}

export async function refreshSession(): Promise<Session | null> {
  if (!refreshing) {
    refreshing = (async () => {
      try {
        const response = await fetch(`${base}/auth/refresh`, {
          method: "POST",
          credentials: "include",
          headers: { "X-Requested-With": "XMLHttpRequest" },
        });
        if (!response.ok) {
          accessToken = null;
          return null;
        }
        const session = (await response.json()) as Session;
        accessToken = session.accessToken;
        return session;
      } catch {
        return null;
      } finally {
        refreshing = null;
      }
    })();
  }
  return refreshing;
}

type Options = {
  method?: string;
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined | null>;
};

export async function api<T>(path: string, options: Options = {}): Promise<T> {
  const url = new URL(base + path, window.location.origin);
  for (const [key, value] of Object.entries(options.query ?? {})) {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.set(key, String(value));
    }
  }

  const send = () => {
    const headers: Record<string, string> = {};
    const isForm = typeof FormData !== "undefined" && options.body instanceof FormData;
    if (options.body !== undefined && !isForm) headers["Content-Type"] = "application/json";
    if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
    return fetch(url.toString(), {
      method: options.method ?? "GET",
      headers,
      credentials: "include",
      body: options.body === undefined ? undefined : isForm ? (options.body as FormData) : JSON.stringify(options.body),
    });
  };

  let response = await send();
  if (response.status === 401 && !path.startsWith("/auth/")) {
    const session = await refreshSession();
    if (!session) {
      onSessionLost?.();
      throw await toError(response);
    }
    response = await send();
  }
  if (!response.ok) throw await toError(response);
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export async function openStream(path: string, signal: AbortSignal): Promise<Response> {
  const send = () =>
    fetch(base + path, {
      headers: { Accept: "text/event-stream", ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}) },
      credentials: "include",
      cache: "no-store",
      signal,
    });
  let response = await send();
  if (response.status === 401) {
    const session = await refreshSession();
    if (!session) {
      onSessionLost?.();
      return response;
    }
    response = await send();
  }
  return response;
}

export async function login(email: string, password: string): Promise<Session> {
  const response = await fetch(`${base}/auth/login`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) throw await toError(response);
  const session = (await response.json()) as Session;
  accessToken = session.accessToken;
  return session;
}

export async function logout(): Promise<void> {
  try {
    await fetch(`${base}/auth/logout`, {
      method: "POST",
      credentials: "include",
      headers: {
        "X-Requested-With": "XMLHttpRequest",
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
    });
  } finally {
    accessToken = null;
  }
}

export async function apiBlob(path: string): Promise<{ blob: Blob; fileName: string }> {
  const url = base + path;
  const send = () =>
    fetch(url, {
      credentials: "include",
      headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
    });
  let response = await send();
  if (response.status === 401) {
    const session = await refreshSession();
    if (!session) {
      onSessionLost?.();
      throw await toError(response);
    }
    response = await send();
  }
  if (!response.ok) throw await toError(response);
  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
  return {
    blob: await response.blob(),
    fileName: match ? decodeURIComponent(match[1]) : "resume",
  };
}

export async function publicApi<T>(path: string, body?: FormData): Promise<T> {
  const response = await fetch(`${base}/public${path}`, {
    method: body ? "POST" : "GET",
    body,
  });
  if (!response.ok) throw await toError(response);
  return (await response.json()) as T;
}
