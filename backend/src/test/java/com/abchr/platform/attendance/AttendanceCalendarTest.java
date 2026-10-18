package com.abchr.platform.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import com.abchr.platform.attendance.AttendanceCalendar;
import com.abchr.platform.attendance.LeaveDay;

class AttendanceCalendarTest {

    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 17);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 18);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 20);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    private static AttendanceCalendar.Context context(Map<LocalDate, String> holidays,
            Map<LocalDate, LeaveDay> leaves) {
        return new AttendanceCalendar.Context(holidays, Map.of(), Set.of(DayOfWeek.SUNDAY), leaves);
    }

    @Test
    void sundayIsWeeklyOff() {
        assertEquals("WEEKLY_OFF", AttendanceCalendar.dayType(SUNDAY, context(Map.of(), Map.of())));
    }

    @Test
    void weekdayIsWorking() {
        assertEquals("WORKING", AttendanceCalendar.dayType(MONDAY, context(Map.of(), Map.of())));
    }

    @Test
    void holidayWinsOverWorkingDay() {
        assertEquals("HOLIDAY",
                AttendanceCalendar.dayType(MONDAY, context(Map.of(MONDAY, "Festival"), Map.of())));
    }

    @Test
    void missingRecordInThePastIsAbsent() {
        assertEquals("ABSENT",
                AttendanceCalendar.state(null, context(Map.of(), Map.of()), SATURDAY, TODAY));
    }

    @Test
    void missingRecordTodayIsNone() {
        assertEquals("NONE", AttendanceCalendar.state(null, context(Map.of(), Map.of()), TODAY, TODAY));
    }

    @Test
    void missingRecordInTheFutureIsUpcoming() {
        assertEquals("UPCOMING",
                AttendanceCalendar.state(null, context(Map.of(), Map.of()), TODAY.plusDays(2), TODAY));
    }

    @Test
    void fullDayLeaveShowsLeave() {
        Map<LocalDate, LeaveDay> leaves = Map.of(TUESDAY, new LeaveDay("Casual", 1.0, false));
        assertEquals("LEAVE", AttendanceCalendar.state(null, context(Map.of(), leaves), TUESDAY, TODAY));
    }

    @Test
    void halfDayLeaveWithoutRecordStaysAbsent() {
        Map<LocalDate, LeaveDay> leaves = Map.of(TUESDAY, new LeaveDay("Casual", 0.5, false));
        assertEquals("ABSENT", AttendanceCalendar.state(null, context(Map.of(), leaves), TUESDAY, TODAY));
    }

    @Test
    void leaveOnWeeklyOffIsIgnored() {
        Map<LocalDate, LeaveDay> leaves = Map.of(SUNDAY, new LeaveDay("Casual", 1.0, false));
        assertEquals("WEEKLY_OFF", AttendanceCalendar.state(null, context(Map.of(), leaves), SUNDAY, TODAY));
    }
}
