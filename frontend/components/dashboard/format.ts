export function compactMoney(value: number): string {
  const sign = value < 0 ? "-" : "";
  const abs = Math.abs(value);
  const trim = (n: number) => String(Math.round(n * 10) / 10);
  if (abs >= 10000000) return `${sign}₹ ${trim(abs / 10000000)} Cr`;
  if (abs >= 100000) return `${sign}₹ ${trim(abs / 100000)}L`;
  if (abs >= 1000) return `${sign}₹ ${trim(abs / 1000)}K`;
  return `${sign}₹ ${Math.round(abs)}`;
}

export function fullMoney(value: number): string {
  return `₹ ${new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 }).format(value)}`;
}

export function monthLabel(month: string): string {
  const [year, number] = month.split("-").map(Number);
  return new Date(year, number - 1, 1).toLocaleDateString(undefined, { month: "short" });
}
