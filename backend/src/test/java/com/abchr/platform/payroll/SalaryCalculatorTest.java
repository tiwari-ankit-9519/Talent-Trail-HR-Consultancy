package com.abchr.platform.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import com.abchr.platform.payroll.PayrollSettings;
import com.abchr.platform.payroll.SalaryCalculator;
import com.abchr.platform.payroll.PayrollDtos.Breakdown;

class SalaryCalculatorTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("1.00");

    private static void assertClose(BigDecimal expected, BigDecimal actual) {
        assertTrue(expected.subtract(actual).abs().compareTo(TOLERANCE) <= 0,
                "expected " + expected + " but was " + actual);
    }

    private static BigDecimal monthlyCost(Breakdown b) {
        return b.gross().add(b.employerPf()).add(b.gratuity());
    }

    @Test
    void splitsAnnualCtcIntoFixedAndVariable() {
        Breakdown b = SalaryCalculator.compute(new BigDecimal("1800000"), new BigDecimal("10"), true,
                new PayrollSettings());
        assertEquals(0, new BigDecimal("180000.00").compareTo(b.variableAnnual()));
        assertEquals(0, new BigDecimal("1620000.00").compareTo(b.fixedAnnual()));
    }

    @Test
    void grossPlusEmployerCostsEqualsFixedMonthly() {
        Breakdown b = SalaryCalculator.compute(new BigDecimal("1800000"), new BigDecimal("10"), true,
                new PayrollSettings());
        assertClose(new BigDecimal("135000"), monthlyCost(b));
    }

    @Test
    void componentsAddUpToGross() {
        Breakdown b = SalaryCalculator.compute(new BigDecimal("1800000"), new BigDecimal("10"), true,
                new PayrollSettings());
        assertEquals(0, b.gross().compareTo(b.basic().add(b.hra()).add(b.allowance())));
    }

    @Test
    void employerPfIsCappedAtWageCeiling() {
        Breakdown b = SalaryCalculator.compute(new BigDecimal("1800000"), new BigDecimal("10"), true,
                new PayrollSettings());
        assertEquals(0, new BigDecimal("1800.00").compareTo(b.employerPf()));
    }

    @Test
    void matchesWorkedExample() {
        Breakdown b = SalaryCalculator.compute(new BigDecimal("1800000"), new BigDecimal("10"), true,
                new PayrollSettings());
        assertClose(new BigDecimal("130071"), b.gross());
        assertClose(new BigDecimal("3128.30"), b.gratuity());
    }

    @Test
    void lowSalaryUsesUncappedPf() {
        Breakdown b = SalaryCalculator.compute(new BigDecimal("240000"), BigDecimal.ZERO, true,
                new PayrollSettings());
        assertTrue(b.employerPf().compareTo(new BigDecimal("1800.00")) <= 0);
        assertClose(new BigDecimal("20000"), monthlyCost(b));
    }

    @Test
    void noPfWhenEmployeeNotCovered() {
        Breakdown b = SalaryCalculator.compute(new BigDecimal("600000"), BigDecimal.ZERO, false,
                new PayrollSettings());
        assertEquals(0, BigDecimal.ZERO.compareTo(b.employerPf()));
    }

    @Test
    void zeroCtcGivesZeroEverything() {
        Breakdown b = SalaryCalculator.compute(BigDecimal.ZERO, BigDecimal.ZERO, true, new PayrollSettings());
        assertEquals(0, BigDecimal.ZERO.compareTo(b.gross()));
        assertEquals(0, BigDecimal.ZERO.compareTo(b.allowance()));
    }

    @Test
    void fullyVariableCtcLeavesNoFixedPay() {
        Breakdown b = SalaryCalculator.compute(new BigDecimal("500000"), new BigDecimal("100"), true,
                new PayrollSettings());
        assertEquals(0, BigDecimal.ZERO.compareTo(b.gross()));
    }
}
