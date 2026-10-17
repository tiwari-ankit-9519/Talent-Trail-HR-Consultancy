package com.abchr.platform.invoice;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoice_payment")
public class InvoicePayment extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "invoice_id", nullable = false, updatable = false)
    UUID invoiceId;

    @Column(name = "paid_on", nullable = false)
    LocalDate paidOn;

    @Column(name = "amount_received", nullable = false)
    BigDecimal amountReceived;

    @Column(name = "tds_deducted", nullable = false)
    BigDecimal tdsDeducted;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    PaymentMode mode;

    @Column(name = "reference", length = 100)
    String reference;

    @Column(name = "note", length = 500)
    String note;

    protected InvoicePayment() {
    }

    public static InvoicePayment create() {
        return new InvoicePayment();
    }

    public UUID getId() {
        return id;
    }
}
