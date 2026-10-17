package com.abchr.platform.attendance;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "attendance_settings")
public class AttendanceSettings extends AuditableEntity {

    @Id
    private Short id = 1;

    @Column(name = "shift_start", nullable = false)
    private LocalTime shiftStart = LocalTime.of(10, 0);

    @Column(name = "late_grace_minutes", nullable = false)
    private int lateGraceMinutes = 15;

    @Column(name = "half_day_minutes", nullable = false)
    private int halfDayMinutes = 240;

    @Column(name = "full_day_minutes", nullable = false)
    private int fullDayMinutes = 480;

    @Column(name = "weekly_off", nullable = false, length = 80)
    private String weeklyOff = "SATURDAY,SUNDAY";

    @Column(name = "optional_quota", nullable = false)
    private int optionalQuota = 2;

    @Column(name = "comp_off_valid_days", nullable = false)
    private int compOffValidDays = 60;

    protected AttendanceSettings() {
    }

    public LocalTime getShiftStart() {
        return shiftStart;
    }

    public int getLateGraceMinutes() {
        return lateGraceMinutes;
    }

    public int getHalfDayMinutes() {
        return halfDayMinutes;
    }

    public int getFullDayMinutes() {
        return fullDayMinutes;
    }

    public int getOptionalQuota() {
        return optionalQuota;
    }

    public int getCompOffValidDays() {
        return compOffValidDays;
    }

    public Set<DayOfWeek> weeklyOffDays() {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (weeklyOff == null || weeklyOff.isBlank()) {
            return days;
        }
        for (String part : weeklyOff.split(",")) {
            String name = part.trim();
            if (!name.isEmpty()) {
                days.add(DayOfWeek.valueOf(name));
            }
        }
        return days;
    }

    public void apply(LocalTime shiftStart, int lateGraceMinutes, int halfDayMinutes, int fullDayMinutes,
            Set<DayOfWeek> offDays, int optionalQuota, int compOffValidDays) {
        this.shiftStart = shiftStart;
        this.lateGraceMinutes = lateGraceMinutes;
        this.halfDayMinutes = halfDayMinutes;
        this.fullDayMinutes = fullDayMinutes;
        this.optionalQuota = optionalQuota;
        this.compOffValidDays = compOffValidDays;
        StringBuilder text = new StringBuilder();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (offDays.contains(day)) {
                if (text.length() > 0) {
                    text.append(',');
                }
                text.append(day.name());
            }
        }
        this.weeklyOff = text.toString();
    }
}
