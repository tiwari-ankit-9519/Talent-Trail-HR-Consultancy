import { Card } from "@/components/shadcn/card";

export function StatCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <Card className="grid gap-1 p-4">
      <span className="muted text-sm">{label}</span>
      <strong className="text-xl">{value}</strong>
      {hint && <span className="muted text-xs">{hint}</span>}
    </Card>
  );
}
