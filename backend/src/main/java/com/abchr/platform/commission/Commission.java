package com.abchr.platform.commission;

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
@Table(name = "commission")
public class Commission extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "placement_id", nullable = false, updatable = false)
    UUID placementId;

    @Column(name = "recruiter_user_id", updatable = false)
    UUID recruiterUserId;

    @Column(name = "recruiter_email", updatable = false, length = 255)
    String recruiterEmail;

    @Column(name = "recruiter_name", updatable = false, length = 200)
    String recruiterName;

    @Column(name = "amount")
    BigDecimal amount;

    @Column(name = "period_days", nullable = false, updatable = false)
    int periodDays;

    @Column(name = "payable_after", nullable = false, updatable = false)
    LocalDate payableAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    CommissionStatus status = CommissionStatus.PENDING;

    @Column(name = "paid_on")
    LocalDate paidOn;

    @Column(name = "payment_reference", length = 100)
    String paymentReference;

    @Column(name = "cancel_reason", length = 500)
    String cancelReason;

    protected Commission() {
    }

    public static Commission create() {
        return new Commission();
    }

    public UUID getId() {
        return id;
    }
}
