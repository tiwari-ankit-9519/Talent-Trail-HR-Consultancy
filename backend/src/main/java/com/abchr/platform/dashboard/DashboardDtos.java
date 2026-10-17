package com.abchr.platform.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record CountPoint(String month, long count) {
    }

    public record MoneyPoint(String month, BigDecimal invoiced, BigDecimal collected) {
    }

    public record Pipeline(
            long openJobs,
            long candidates,
            long availableCandidates,
            Map<String, Long> submissionsByStatus,
            long interviewsNext7Days,
            long offersPending,
            long joiningsThisMonth,
            List<CountPoint> joiningsTrend) {
    }

    public record Money(
            BigDecimal invoicedThisMonth,
            BigDecimal collectedThisMonth,
            BigDecimal outstanding,
            long overdueCount,
            BigDecimal overdueAmount,
            long readyToInvoice,
            long awaitingJoiningEmail,
            List<MoneyPoint> trend) {
    }

    public record DashboardResponse(Pipeline pipeline, Money money) {
    }
}
