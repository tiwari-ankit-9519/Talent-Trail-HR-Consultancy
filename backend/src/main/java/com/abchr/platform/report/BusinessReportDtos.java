package com.abchr.platform.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class BusinessReportDtos {

    private BusinessReportDtos() {
    }

    public record PayrollRow(
            UUID userId,
            String name,
            String email,
            String employeeCode,
            String designation,
            BigDecimal payableDays,
            BigDecimal lossOfPayDays,
            BigDecimal gross,
            BigDecimal earnedGross,
            BigDecimal basic,
            BigDecimal hra,
            BigDecimal allowance,
            BigDecimal commission,
            BigDecimal otherEarnings,
            BigDecimal pf,
            BigDecimal pt,
            BigDecimal tds,
            BigDecimal otherDeductions,
            BigDecimal net,
            String bankAccountHolder,
            String bankName,
            String bankIfsc,
            String bankAccountNumber,
            String pan) {
    }

    public record PayrollTotals(
            BigDecimal earnedGross,
            BigDecimal commission,
            BigDecimal otherEarnings,
            BigDecimal pf,
            BigDecimal pt,
            BigDecimal tds,
            BigDecimal otherDeductions,
            BigDecimal net) {
    }

    public record PayrollRegister(
            String month, String status, boolean bankIncluded, List<PayrollRow> rows, PayrollTotals totals) {
    }

    public record LeaveTypeColumn(UUID id, String name, boolean unpaid, double yearlyDays) {
    }

    public record LeaveCell(UUID typeId, double approved, double pending, Double remaining) {
    }

    public record LeaveRow(
            UUID userId, String name, String email, List<LeaveCell> cells, double totalApproved, double totalPending) {
    }

    public record LeaveReport(int year, List<LeaveTypeColumn> types, List<LeaveRow> rows) {
    }

    public record ReceivableRow(
            UUID invoiceId,
            String number,
            String clientName,
            String candidateName,
            String status,
            LocalDate invoiceDate,
            LocalDate dueDate,
            BigDecimal total,
            BigDecimal settled,
            BigDecimal outstanding,
            long daysOverdue,
            String bucket) {
    }

    public record ReceivablesReport(
            LocalDate asOf,
            BigDecimal outstanding,
            BigDecimal overdue,
            ReportDtos.Aging aging,
            List<ReceivableRow> rows) {
    }

    public record TaxMonth(
            String month,
            BigDecimal baseAmount,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal total,
            BigDecimal tdsExpected,
            BigDecimal tdsDeducted,
            BigDecimal received) {
    }

    public record TaxSummary(List<TaxMonth> months, TaxMonth totals) {
    }

    public record CommissionRow(
            UUID commissionId,
            String recruiter,
            String candidateName,
            String clientName,
            String jobCode,
            LocalDate joiningDate,
            BigDecimal amount,
            String status,
            String state,
            LocalDate payableAfter,
            LocalDate paidOn,
            String paymentReference,
            String invoiceNumber,
            String invoiceStatus) {
    }

    public record RecruiterCommission(
            String recruiter, long count, BigDecimal payable, BigDecimal waiting, BigDecimal paid) {
    }

    public record CommissionReport(
            BigDecimal payable,
            BigDecimal waiting,
            BigDecimal paid,
            List<RecruiterCommission> byRecruiter,
            List<CommissionRow> rows) {
    }
}
