package com.abchr.platform.attendance;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "attendance")
public class Attendance extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "work_date", nullable = false, updatable = false)
    LocalDate workDate;

    @Column(name = "check_in")
    Instant checkIn;

    @Column(name = "check_out")
    Instant checkOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 10)
    WorkMode workMode = WorkMode.OFFICE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "late", nullable = false)
    boolean late;

    @Column(name = "worked_minutes")
    Integer workedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    AttendanceSource source = AttendanceSource.SELF;

    @Column(name = "note", length = 300)
    String note;

    protected Attendance() {
    }

    public static Attendance create(UUID userId, LocalDate workDate) {
        Attendance record = new Attendance();
        record.userId = userId;
        record.workDate = workDate;
        return record;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public Instant getCheckIn() {
        return checkIn;
    }

    public Instant getCheckOut() {
        return checkOut;
    }

    public WorkMode getWorkMode() {
        return workMode;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public boolean isLate() {
        return late;
    }

    public Integer getWorkedMinutes() {
        return workedMinutes;
    }

    public AttendanceSource getSource() {
        return source;
    }

    public String getNote() {
        return note;
    }

    void evaluate(AttendanceSettings settings, ZoneId zone) {
        if (checkIn == null) {
            late = false;
            workedMinutes = null;
            return;
        }
        LocalTime start = checkIn.atZone(zone).toLocalTime();
        late = start.isAfter(settings.getShiftStart().plusMinutes(settings.getLateGraceMinutes()));
        if (checkOut == null) {
            workedMinutes = null;
            status = AttendanceStatus.PRESENT;
            return;
        }
        int minutes = (int) Duration.between(checkIn, checkOut).toMinutes();
        workedMinutes = minutes;
        if (minutes >= settings.getFullDayMinutes()) {
            status = AttendanceStatus.PRESENT;
        } else if (minutes >= settings.getHalfDayMinutes()) {
            status = AttendanceStatus.HALF_DAY;
        } else {
            status = AttendanceStatus.ABSENT;
        }
    }
}
