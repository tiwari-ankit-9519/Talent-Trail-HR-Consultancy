package com.abchr.platform.attendance;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "attendance_request")
public class AttendanceRequest extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "work_date", nullable = false, updatable = false)
    LocalDate workDate;

    @Column(name = "check_in", nullable = false, updatable = false)
    LocalTime checkIn;

    @Column(name = "check_out", updatable = false)
    LocalTime checkOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, updatable = false, length = 10)
    WorkMode workMode = WorkMode.OFFICE;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    RequestStatus status = RequestStatus.PENDING;

    @Column(name = "decided_by", length = 255)
    String decidedBy;

    @Column(name = "decided_at")
    Instant decidedAt;

    @Column(name = "decision_note", length = 300)
    String decisionNote;

    protected AttendanceRequest() {
    }

    public static AttendanceRequest create() {
        return new AttendanceRequest();
    }

    public UUID getId() {
        return id;
    }
}
