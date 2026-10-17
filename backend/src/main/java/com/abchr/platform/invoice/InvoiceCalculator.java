package com.abchr.platform.invoice;

import com.abchr.platform.client.FeeType;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class InvoiceCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    public record Amounts(
            BigDecimal base,
            BigDecimal gstRate,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal total,
            BigDecimal tdsRate,
            BigDecimal tds,
            BigDecimal net) {
    }

    private InvoiceCalculator() {
    }

    public static BigDecimal fee(FeeType type, BigDecimal feeValue, BigDecimal ctc) {
        BigDecimal raw = type == FeeType.MONTHS_OF_SALARY
                ? ctc.multiply(feeValue).divide(TWELVE, 10, RoundingMode.HALF_UP)
                : ctc.multiply(feeValue).divide(HUNDRED, 10, RoundingMode.HALF_UP);
        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    public static Amounts compute(
            BigDecimal base,
            boolean gstApplicable,
            BigDecimal gstRate,
            boolean sameState,
            boolean tdsApplicable,
            BigDecimal tdsRate) {
        BigDecimal rate = gstApplicable ? gstRate : BigDecimal.ZERO;
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        BigDecimal cgst = zero;
        BigDecimal sgst = zero;
        BigDecimal igst = zero;
        if (rate.signum() > 0) {
            if (sameState) {
                BigDecimal half = base.multiply(rate).divide(HUNDRED.multiply(new BigDecimal("2")), 2,
                        RoundingMode.HALF_UP);
                cgst = half;
                sgst = half;
            } else {
                igst = base.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            }
        }
        BigDecimal total = base.add(cgst).add(sgst).add(igst);
        BigDecimal appliedTdsRate = tdsApplicable ? tdsRate : BigDecimal.ZERO;
        BigDecimal tds = appliedTdsRate.signum() > 0
                ? base.multiply(appliedTdsRate).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : zero;
        return new Amounts(base, rate, cgst, sgst, igst, total, appliedTdsRate, tds, total.subtract(tds));
    }
}
