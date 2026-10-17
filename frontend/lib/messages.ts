import en from "@/messages/en.json";

type Messages = typeof en;

type Join<K extends string, P extends string> = `${K}.${P}`;

type Paths<T> = {
  [K in keyof T & string]: T[K] extends string ? K : Join<K, Paths<T[K]>>;
}[keyof T & string];

export type MessageKey = Paths<Messages>;

export function t(key: MessageKey): string {
  const value = key
    .split(".")
    .reduce<unknown>((node, part) => (node as Record<string, unknown> | undefined)?.[part], en);
  return typeof value === "string" ? value : key;
}

export function tx(key: string, fallback?: string): string {
  const value = key
    .split(".")
    .reduce<unknown>((node, part) => (node as Record<string, unknown> | undefined)?.[part], en);
  return typeof value === "string" ? value : (fallback ?? key);
}

export function errorText(code: string, fallback?: string): string {
  return tx(`errors.${code}`, fallback ?? tx("errors.UNKNOWN"));
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return "";
  return new Date(value).toLocaleString();
}

export function formatDay(value: string | null | undefined): string {
  if (!value) return "";
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString();
}

export function formatMoney(value: number | null | undefined): string {
  if (value === null || value === undefined) return "";
  return `₹ ${new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 }).format(value)}`;
}
