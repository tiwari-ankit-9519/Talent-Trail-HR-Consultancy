package com.abchr.platform.payroll;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payslip")
public class Payslip extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "run_id", nullable = false, updatable = false)
    UUID runId;

    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "employee_name", nullable = false, length = 150)
    String employeeName;

    @Column(name = "employee_email", nullable = false, length = 255)
    String employeeEmail;

    @Column(name = "calendar_days", nullable = false)
    int calendarDays;

    @Column(name = "payable_days", nullable = false)
    BigDecimal payableDays = BigDecimal.ZERO;

    @Column(name = "leave_days", nullable = false)
    BigDecimal leaveDays = BigDecimal.ZERO;

    @Column(name = "loss_of_pay_days", nullable = false)
    BigDecimal lossOfPayDays = BigDecimal.ZERO;

    @Column(name = "gross", nullable = false)
    BigDecimal gross = BigDecimal.ZERO;

    @Column(name = "earned_gross", nullable = false)
    BigDecimal earnedGross = BigDecimal.ZERO;

    @Column(name = "basic")
    BigDecimal basic;

    @Column(name = "hra")
    BigDecimal hra;

    @Column(name = "allowance")
    BigDecimal allowance;

    @Column(name = "commission", nullable = false)
    BigDecimal commission = BigDecimal.ZERO;

    @Column(name = "other_earnings", nullable = false)
    BigDecimal otherEarnings = BigDecimal.ZERO;

    @Column(name = "earnings_note", length = 200)
    String earningsNote;

    @Column(name = "pf", nullable = false)
    BigDecimal pf = BigDecimal.ZERO;

    @Column(name = "pt", nullable = false)
    BigDecimal pt = BigDecimal.ZERO;

    @Column(name = "tds", nullable = false)
    BigDecimal tds = BigDecimal.ZERO;

    @Column(name = "other_deductions", nullable = false)
    BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "deductions_note", length = 200)
    String deductionsNote;

    @Column(name = "net", nullable = false)
    BigDecimal net = BigDecimal.ZERO;

    protected Payslip() {
    }

    static Payslip create(UUID runId, UUID userId) {
        Payslip payslip = new Payslip();
        payslip.runId = runId;
        payslip.userId = userId;
        return payslip;
    }

    public UUID getId() {
        return id;
    }
}
