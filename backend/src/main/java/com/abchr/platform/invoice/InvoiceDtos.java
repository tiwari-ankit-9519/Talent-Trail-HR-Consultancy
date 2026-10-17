package com.abchr.platform.invoice;

import com.abchr.platform.shared.mail.EmailStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InvoiceDtos {

    private InvoiceDtos() {
    }

    public record CreateInvoiceRequest(@NotNull UUID placementId) {
    }

    public record CancelRequest(@NotBlank @Size(max = 500) String reason, Long version) {
    }

    public record SendRequest(@NotEmpty @Size(max = 10) List<@NotBlank @Email @Size(max = 255) String> to) {
    }

    public record PaymentRequest(
            @NotNull LocalDate paidOn,
            @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal amountReceived,
            @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal tdsDeducted,
            @NotNull PaymentMode mode,
            @Size(max = 100) String reference,
            @Size(max = 500) String note) {
    }

    public record InvoiceSummary(
            UUID id,
            String number,
            InvoiceStatus status,
            boolean overdue,
            UUID clientId,
            String clientName,
            String candidateName,
            String jobCode,
            LocalDate invoiceDate,
            LocalDate dueDate,
            BigDecimal totalAmount,
            BigDecimal settledAmount,
            BigDecimal balance,
            EmailStatus emailStatus) {
    }

    public record PaymentView(
            UUID id,
            LocalDate paidOn,
            BigDecimal amountReceived,
            BigDecimal tdsDeducted,
            PaymentMode mode,
            String reference,
            String note,
            String recordedBy) {
    }

    public record ContactView(String name, String email, boolean primary) {
    }

    public record InvoiceDetail(
            UUID id,
            String number,
            InvoiceStatus status,
            boolean overdue,
            UUID placementId,
            UUID clientId,
            String clientName,
            UUID jobId,
            String jobCode,
            String jobTitle,
            UUID candidateId,
            String candidateName,
            LocalDate invoiceDate,
            LocalDate dueDate,
            LocalDate joiningDate,
            String description,
            String sacCode,
            BigDecimal offeredCtc,
            String feeType,
            BigDecimal feeValue,
            BigDecimal baseAmount,
            BigDecimal gstRate,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal totalAmount,
            BigDecimal tdsRate,
            BigDecimal tdsAmount,
            BigDecimal netReceivable,
            BigDecimal settledAmount,
            BigDecimal balance,
            String cancelReason,
            Instant cancelledAt,
            EmailStatus emailStatus,
            Instant emailSentAt,
            String emailError,
            String sentTo,
            List<PaymentView> payments,
            List<ContactView> contacts,
            Long version) {
    }
}
