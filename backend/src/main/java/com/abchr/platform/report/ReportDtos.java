package com.abchr.platform.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record CountPoint(String month, long total) {
    }

    public record MoneyPoint(String month, BigDecimal invoiced, BigDecimal collected) {
    }

    public record JoiningRow(
            UUID placementId,
            LocalDate joiningDate,
            String candidateName,
            String clientName,
            String jobCode,
            String jobTitle,
            String recruiter,
            BigDecimal offeredCtc,
            String invoiceNumber,
            BigDecimal invoiceTotal,
            String invoiceStatus) {
    }

    public record JoiningsReport(
            long count,
            BigDecimal totalCtc,
            BigDecimal totalInvoiced,
            List<CountPoint> byMonth,
            List<JoiningRow> rows) {
    }

    public record Aging(
            BigDecimal notDue, BigDecimal days1to30, BigDecimal days31to60, BigDecimal days61to90,
            BigDecimal days90plus) {
    }

    public record RevenueRow(
            UUID clientId,
            String clientName,
            BigDecimal invoiced,
            BigDecimal collected,
            BigDecimal outstanding,
            BigDecimal overdue) {
    }

    public record RevenueReport(
            BigDecimal invoiced,
            BigDecimal collected,
            BigDecimal outstanding,
            BigDecimal overdue,
            Aging aging,
            List<MoneyPoint> trend,
            List<RevenueRow> rows) {
    }

    public record RecruiterRow(
            String email,
            String name,
            long submissions,
            long selected,
            long rejected,
            long joined,
            BigDecimal commissionTotal,
            BigDecimal commissionPaid) {
    }

    public record FunnelStage(String key, long count) {
    }

    public record FunnelReport(List<FunnelStage> stages, Double averageDaysToJoin) {
    }

    public record ReplacementRow(
            UUID clientId, String clientName, long joined, long exits, long withinPeriod, long filled, long open) {
    }
}
