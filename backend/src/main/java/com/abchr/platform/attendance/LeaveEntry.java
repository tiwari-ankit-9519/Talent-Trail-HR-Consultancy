package com.abchr.platform.attendance;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_entry")
public class LeaveEntry extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "leave_date", nullable = false, updatable = false)
    LocalDate leaveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false, length = 20)
    LeaveKind kind;

    @Column(name = "holiday_id", updatable = false)
    UUID holidayId;

    @Column(name = "comp_off_id", updatable = false)
    UUID compOffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "decided_by", length = 255)
    String decidedBy;

    @Column(name = "decided_at")
    java.time.Instant decidedAt;

    @Column(name = "decision_note", length = 300)
    String decisionNote;

    protected LeaveEntry() {
    }

    public static LeaveEntry create(UUID userId, LocalDate date, LeaveKind kind, UUID holidayId, UUID compOffId) {
        LeaveEntry entry = new LeaveEntry();
        entry.userId = userId;
        entry.leaveDate = date;
        entry.kind = kind;
        entry.holidayId = holidayId;
        entry.compOffId = compOffId;
        return entry;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getLeaveDate() {
        return leaveDate;
    }

    public LeaveKind getKind() {
        return kind;
    }
}
