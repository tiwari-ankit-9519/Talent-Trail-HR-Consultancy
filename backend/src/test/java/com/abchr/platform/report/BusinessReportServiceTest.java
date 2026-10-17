package com.abchr.platform.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BusinessReportServiceTest {

    private final LocalDate today = LocalDate.of(2026, 10, 18);

    @Test
    void invoiceDueTodayOrLaterIsNotDue() {
        assertEquals("NOT_DUE", BusinessReportService.bucket(today, today));
        assertEquals("NOT_DUE", BusinessReportService.bucket(today.plusDays(5), today));
    }

    @Test
    void overdueInvoicesFallIntoTheRightBucket() {
        assertEquals("D1_30", BusinessReportService.bucket(today.minusDays(1), today));
        assertEquals("D1_30", BusinessReportService.bucket(today.minusDays(30), today));
        assertEquals("D31_60", BusinessReportService.bucket(today.minusDays(31), today));
        assertEquals("D61_90", BusinessReportService.bucket(today.minusDays(90), today));
        assertEquals("D90_PLUS", BusinessReportService.bucket(today.minusDays(91), today));
    }
}
