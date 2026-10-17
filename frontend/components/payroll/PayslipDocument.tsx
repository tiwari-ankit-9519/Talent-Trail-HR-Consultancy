import { formatMoney, t } from "@/lib/messages";
import type { Payslip } from "@/lib/types";

function monthTitle(month: string): string {
  const [year, number] = month.split("-").map(Number);
  return new Date(year, number - 1, 1).toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

function Row({ label, value, strong }: { label: string; value: number; strong?: boolean }) {
  return (
    <div className={`flex items-center justify-between gap-3 py-1 text-sm ${strong ? "font-semibold" : ""}`}>
      <span>{label}</span>
      <span className="tabular-nums">{formatMoney(value)}</span>
    </div>
  );
}

export default function PayslipDocument({ slip }: { slip: Payslip }) {
  const showBreakup = slip.basic !== null;
  return (
    <article className="grid gap-4 text-fg">
      <header className="flex flex-wrap items-start justify-between gap-3 border-b border-line pb-3">
        <div>
          <p className="m-0 text-xs uppercase tracking-wide text-muted">{t("app.title")}</p>
          <h2 className="m-0 text-lg font-semibold">
            {t("payroll.payslip.title").replace("{month}", monthTitle(slip.month))}
          </h2>
        </div>
        <div className="text-right text-sm">
          <p className="m-0 font-semibold">{slip.employeeName}</p>
          <p className="m-0 text-muted">{slip.employeeEmail}</p>
        </div>
      </header>

      <section className="grid grid-cols-2 gap-x-6 gap-y-1 text-sm sm:grid-cols-4">
        <div>
          <p className="m-0 text-xs text-muted">{t("payroll.payslip.calendarDays")}</p>
          <p className="m-0 font-medium">{slip.calendarDays}</p>
        </div>
        <div>
          <p className="m-0 text-xs text-muted">{t("payroll.payslip.payableDays")}</p>
          <p className="m-0 font-medium">{slip.payableDays}</p>
        </div>
        <div>
          <p className="m-0 text-xs text-muted">{t("payroll.payslip.leaveDays")}</p>
          <p className="m-0 font-medium">{slip.leaveDays}</p>
        </div>
        <div>
          <p className="m-0 text-xs text-muted">{t("payroll.payslip.lossOfPay")}</p>
          <p className="m-0 font-medium">{slip.lossOfPayDays}</p>
        </div>
      </section>

      <section className="grid gap-6 sm:grid-cols-2">
        <div>
          <h3 className="m-0 mb-1 border-b border-line pb-1 text-sm font-semibold">{t("payroll.payslip.earnings")}</h3>
          {showBreakup ? (
            <>
              <Row label={t("payroll.payslip.basic")} value={slip.basic ?? 0} />
              {slip.hra !== null && <Row label={t("payroll.payslip.hra")} value={slip.hra} />}
              <Row label={t("payroll.payslip.allowance")} value={slip.allowance ?? 0} />
            </>
          ) : (
            <Row label={t("payroll.payslip.grossEarned")} value={slip.earnedGross} />
          )}
          {slip.commissions.map((line) => (
            <Row key={line.commissionId} label={line.label} value={line.amount} />
          ))}
          {slip.otherEarnings > 0 && (
            <Row label={slip.earningsNote ?? t("payroll.payslip.otherEarnings")} value={slip.otherEarnings} />
          )}
          <div className="mt-1 border-t border-line pt-1">
            <Row label={t("payroll.payslip.totalEarnings")} value={slip.totalEarnings} strong />
          </div>
        </div>
        <div>
          <h3 className="m-0 mb-1 border-b border-line pb-1 text-sm font-semibold">
            {t("payroll.payslip.deductions")}
          </h3>
          <Row label={t("payroll.payslip.pf")} value={slip.pf} />
          <Row label={t("payroll.payslip.pt")} value={slip.pt} />
          <Row label={t("payroll.payslip.tds")} value={slip.tds} />
          {slip.otherDeductions > 0 && (
            <Row label={slip.deductionsNote ?? t("payroll.payslip.otherDeductions")} value={slip.otherDeductions} />
          )}
          <div className="mt-1 border-t border-line pt-1">
            <Row label={t("payroll.payslip.totalDeductions")} value={slip.totalDeductions} strong />
          </div>
        </div>
      </section>

      <footer className="flex items-center justify-between gap-3 rounded-lg border border-line bg-bg px-4 py-3">
        <span className="text-sm font-semibold">{t("payroll.payslip.net")}</span>
        <span className="text-xl font-semibold tabular-nums">{formatMoney(slip.net)}</span>
      </footer>
      <p className="m-0 text-xs text-muted">{t("payroll.payslip.generated")}</p>
    </article>
  );
}
