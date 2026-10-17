package com.abchr.platform.attendance;

import com.abchr.platform.attendance.AttendanceDtos.DayView;
import com.abchr.platform.attendance.AttendanceDtos.Totals;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AttendanceCalendar {

    record Context(
            Map<LocalDate, String> holidays,
            Map<LocalDate, String> optional,
            Set<DayOfWeek> weeklyOff,
            Map<LocalDate, LeaveEntry> leaves) {

        Context withLeaves(Map<LocalDate, LeaveEntry> other) {
            return new Context(holidays, optional, weeklyOff, other);
        }
    }

    private AttendanceCalendar() {
    }

    static String dayType(LocalDate date, Context context) {
        if (context.holidays().containsKey(date)) {
            return "HOLIDAY";
        }
        if (context.weeklyOff().contains(date.getDayOfWeek())) {
            return "WEEKLY_OFF";
        }
        return "WORKING";
    }

    static String state(Attendance record, Context context, LocalDate date, LocalDate today) {
        if (record != null) {
            return record.getStatus().name();
        }
        String type = dayType(date, context);
        if (!"WORKING".equals(type)) {
            return type;
        }
        if (context.leaves().containsKey(date)) {
            return "LEAVE";
        }
        if (date.isAfter(today)) {
            return "UPCOMING";
        }
        if (date.equals(today)) {
            return "NONE";
        }
        return "ABSENT";
    }

    static List<DayView> month(
            YearMonth month,
            LocalDate today,
            Map<LocalDate, Attendance> records,
            Context context,
            Set<LocalDate> pending) {
        List<DayView> days = new ArrayList<>();
        for (int i = 1; i <= month.lengthOfMonth(); i++) {
            LocalDate date = month.atDay(i);
            Attendance record = records.get(date);
            LeaveEntry leave = context.leaves().get(date);
            days.add(new DayView(
                    date,
                    state(record, context, date, today),
                    context.holidays().get(date),
                    record == null ? null : record.getWorkMode(),
                    record == null ? null : record.getCheckIn(),
                    record == null ? null : record.getCheckOut(),
                    record == null ? null : record.getWorkedMinutes(),
                    record != null && record.isLate(),
                    pending.contains(date),
                    record == null ? null : record.getNote(),
                    context.optional().get(date),
                    leave == null ? null : leave.getKind()));
        }
        return days;
    }

    static Totals totals(List<DayView> days) {
        int present = 0;
        int half = 0;
        int absent = 0;
        int late = 0;
        int wfh = 0;
        int holidays = 0;
        int offs = 0;
        int leaves = 0;
        int minutes = 0;
        for (DayView day : days) {
            switch (day.state()) {
                case "PRESENT" -> present++;
                case "HALF_DAY" -> half++;
                case "ABSENT" -> absent++;
                case "HOLIDAY" -> holidays++;
                case "WEEKLY_OFF" -> offs++;
                case "LEAVE" -> leaves++;
                default -> {
                }
            }
            if (day.late()) {
                late++;
            }
            if (day.workMode() == WorkMode.WFH && !"ABSENT".equals(day.state())) {
                wfh++;
            }
            if (day.workedMinutes() != null) {
                minutes += day.workedMinutes();
            }
        }
        double payable = present + half * 0.5 + holidays + offs + leaves;
        return new Totals(present, half, absent, late, wfh, holidays, offs, leaves, minutes, payable);
    }

    static Instant at(LocalDate date, LocalTime time, ZoneId zone) {
        return date.atTime(time).atZone(zone).toInstant();
    }
}
