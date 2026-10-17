package com.abchr.platform.payroll;

import com.abchr.platform.payroll.PayrollDtos.Breakdown;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

final class SalaryCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MONTHS = BigDecimal.valueOf(12);
    private static final MathContext MC = MathContext.DECIMAL64;

    private SalaryCalculator() {
    }

    static Breakdown compute(BigDecimal ctcAnnual, BigDecimal variablePercent, boolean pfApplicable,
            PayrollSettings settings) {
        BigDecimal ctc = ctcAnnual.setScale(2, RoundingMode.HALF_UP);
        BigDecimal variableAnnual = ctc.multiply(variablePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal fixedAnnual = ctc.subtract(variableAnnual);
        BigDecimal fixedMonthly = fixedAnnual.divide(MONTHS, MC);

        BigDecimal basicShare = settings.basicPercent.divide(HUNDRED, MC);
        BigDecimal pfRate = settings.pfEnabled && pfApplicable
                ? settings.employerPfRate.divide(HUNDRED, MC)
                : BigDecimal.ZERO;
        BigDecimal gratuityRate = settings.gratuityEnabled
                ? settings.gratuityPercent.divide(HUNDRED, MC)
                : BigDecimal.ZERO;
        BigDecimal ceiling = settings.pfWageCeiling;

        BigDecimal gross = fixedMonthly.divide(
                BigDecimal.ONE.add(basicShare.multiply(pfRate.add(gratuityRate))), MC);
        boolean capped = pfRate.signum() > 0 && ceiling.signum() > 0 && basicShare.multiply(gross).compareTo(ceiling) > 0;
        if (capped) {
            gross = fixedMonthly.subtract(pfRate.multiply(ceiling))
                    .divide(BigDecimal.ONE.add(basicShare.multiply(gratuityRate)), MC);
        }
        if (gross.signum() < 0) {
            gross = BigDecimal.ZERO;
        }
        gross = gross.setScale(2, RoundingMode.HALF_UP);
        BigDecimal basic = gross.multiply(basicShare).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hra = basic.multiply(settings.hraPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        if (basic.add(hra).compareTo(gross) > 0) {
            hra = gross.subtract(basic);
        }
        BigDecimal allowance = gross.subtract(basic).subtract(hra);
        BigDecimal pfWage = ceiling.signum() > 0 && basic.compareTo(ceiling) > 0 ? ceiling : basic;
        BigDecimal employerPf = pfRate.multiply(pfWage).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gratuity = gratuityRate.multiply(basic).setScale(2, RoundingMode.HALF_UP);
        return new Breakdown(ctc, variablePercent, variableAnnual, fixedAnnual, gross, basic, hra, allowance,
                employerPf, gratuity);
    }
}
