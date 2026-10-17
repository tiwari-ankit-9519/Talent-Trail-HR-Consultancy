package com.abchr.platform.commission;

import com.abchr.platform.invoice.InvoiceStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CommissionDtos {

    private CommissionDtos() {
    }

    public record AmountRequest(
            @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
            Long version) {
    }

    public record PayRequest(
            @NotNull LocalDate paidOn,
            @Size(max = 100) String reference,
            Long version) {
    }

    public record CommissionView(
            UUID id,
            UUID placementId,
            String candidateName,
            String jobCode,
            String clientName,
            UUID recruiterUserId,
            String recruiterName,
            BigDecimal amount,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            CommissionStatus status,
            LocalDate payableAfter,
            int periodDays,
            String invoiceNumber,
            InvoiceStatus invoiceStatus,
            List<String> blockers,
            LocalDate paidOn,
            String paymentReference,
            String cancelReason,
            Long version) {
    }
}
