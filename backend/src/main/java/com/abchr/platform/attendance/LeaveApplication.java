package com.abchr.platform.attendance;

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
@Table(name = "leave_application")
public class LeaveApplication extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "leave_type_id", nullable = false, updatable = false)
    UUID leaveTypeId;

    @Column(name = "type_name", nullable = false, updatable = false, length = 60)
    String typeName;

    @Column(name = "unpaid", nullable = false, updatable = false)
    boolean unpaid;

    @Column(name = "from_date", nullable = false, updatable = false)
    LocalDate fromDate;

    @Column(name = "to_date", nullable = false, updatable = false)
    LocalDate toDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "half_day", updatable = false, length = 10)
    HalfDay halfDay;

    @Column(name = "days", nullable = false, updatable = false)
    BigDecimal days;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "decided_by", length = 255)
    String decidedBy;

    @Column(name = "decided_at")
    Instant decidedAt;

    @Column(name = "decision_note", length = 300)
    String decisionNote;

    protected LeaveApplication() {
    }

    public static LeaveApplication create() {
        return new LeaveApplication();
    }

    public UUID getId() {
        return id;
    }
}
