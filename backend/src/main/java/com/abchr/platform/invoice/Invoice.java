package com.abchr.platform.invoice;

import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoice")
public class Invoice extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "number", nullable = false, updatable = false, length = 40)
    String number;

    @Column(name = "placement_id", nullable = false, updatable = false)
    UUID placementId;

    @Column(name = "client_id", nullable = false, updatable = false)
    UUID clientId;

    @Column(name = "job_id", nullable = false, updatable = false)
    UUID jobId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    InvoiceStatus status = InvoiceStatus.ISSUED;

    @Column(name = "invoice_date", nullable = false, updatable = false)
    LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false, updatable = false)
    LocalDate dueDate;

    @Column(name = "joining_date", nullable = false, updatable = false)
    LocalDate joiningDate;

    @Column(name = "description", nullable = false, updatable = false, length = 600)
    String description;

    @Column(name = "candidate_name", nullable = false, updatable = false, length = 150)
    String candidateName;

    @Column(name = "job_code", nullable = false, updatable = false, length = 20)
    String jobCode;

    @Column(name = "job_title", nullable = false, updatable = false, length = 200)
    String jobTitle;

    @Column(name = "seller_name", nullable = false, updatable = false, length = 250)
    String sellerName;

    @Column(name = "seller_gstin", updatable = false, length = 15)
    String sellerGstin;

    @Column(name = "seller_pan", updatable = false, length = 10)
    String sellerPan;

    @Column(name = "seller_address", updatable = false, columnDefinition = "text")
    String sellerAddress;

    @Column(name = "seller_state", updatable = false, length = 100)
    String sellerState;

    @Column(name = "seller_bank", updatable = false, columnDefinition = "text")
    String sellerBank;

    @Column(name = "sac_code", nullable = false, updatable = false, length = 10)
    String sacCode;

    @Column(name = "buyer_name", nullable = false, updatable = false, length = 250)
    String buyerName;

    @Column(name = "buyer_gstin", updatable = false, length = 15)
    String buyerGstin;

    @Column(name = "buyer_address", updatable = false, columnDefinition = "text")
    String buyerAddress;

    @Column(name = "buyer_state", updatable = false, length = 100)
    String buyerState;

    @Column(name = "offered_ctc", nullable = false, updatable = false)
    BigDecimal offeredCtc;

    @Column(name = "fee_type", nullable = false, updatable = false, length = 30)
    String feeType;

    @Column(name = "fee_value", nullable = false, updatable = false)
    BigDecimal feeValue;

    @Column(name = "base_amount", nullable = false, updatable = false)
    BigDecimal baseAmount;

    @Column(name = "gst_rate", nullable = false, updatable = false)
    BigDecimal gstRate;

    @Column(name = "cgst_amount", nullable = false, updatable = false)
    BigDecimal cgstAmount;

    @Column(name = "sgst_amount", nullable = false, updatable = false)
    BigDecimal sgstAmount;

    @Column(name = "igst_amount", nullable = false, updatable = false)
    BigDecimal igstAmount;

    @Column(name = "total_amount", nullable = false, updatable = false)
    BigDecimal totalAmount;

    @Column(name = "tds_rate", nullable = false, updatable = false)
    BigDecimal tdsRate;

    @Column(name = "tds_amount", nullable = false, updatable = false)
    BigDecimal tdsAmount;

    @Column(name = "net_receivable", nullable = false, updatable = false)
    BigDecimal netReceivable;

    @Column(name = "settled_amount", nullable = false)
    BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(name = "cancel_reason", length = 500)
    String cancelReason;

    @Column(name = "cancelled_at")
    Instant cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    EmailStatus emailStatus = EmailStatus.PENDING;

    @Column(name = "email_sent_at")
    Instant emailSentAt;

    @Column(name = "email_error", length = 500)
    String emailError;

    @Column(name = "sent_to", columnDefinition = "text")
    String sentTo;

    protected Invoice() {
    }

    public static Invoice create() {
        return new Invoice();
    }

    public UUID getId() {
        return id;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getNumber() {
        return number;
    }

    public BigDecimal getBalance() {
        return totalAmount.subtract(settledAmount);
    }
}
