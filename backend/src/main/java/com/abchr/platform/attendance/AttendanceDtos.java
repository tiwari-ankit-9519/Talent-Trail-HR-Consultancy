package com.abchr.platform.attendance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AttendanceDtos {

    public static final String TIME = "^([01]\\d|2[0-3]):[0-5]\\d$";

    private AttendanceDtos() {
    }

    public record SettingsView(
            String shiftStart,
            int lateGraceMinutes,
            int halfDayMinutes,
            int fullDayMinutes,
            List<String> weeklyOff,
            int optionalQuota,
            int compOffValidDays,
            Long version) {
    }

    public record SettingsRequest(
            @NotBlank @Pattern(regexp = TIME) String shiftStart,
            @Min(0) @Max(240) int lateGraceMinutes,
            @Min(30) @Max(600) int halfDayMinutes,
            @Min(60) @Max(1000) int fullDayMinutes,
            @NotNull List<String> weeklyOff,
            @Min(0) @Max(30) int optionalQuota,
            @Min(1) @Max(365) int compOffValidDays,
            Long version) {
    }

    public record HolidayView(UUID id, LocalDate date, String name, HolidayKind kind) {
    }

    public record HolidayRequest(
            @NotNull LocalDate date,
            @NotBlank @Size(max = 150) String name,
            @NotNull HolidayKind kind) {
    }

    public record CheckInRequest(
            WorkMode workMode,
            @Size(max = 300) String note) {
    }

    public record DayView(
            LocalDate date,
            String state,
            String holidayName,
            WorkMode workMode,
            Instant checkIn,
            Instant checkOut,
            Integer workedMinutes,
            boolean late,
            boolean pendingRequest,
            String note,
            String optionalHolidayName,
            LeaveKind leaveKind) {
    }

    public record Totals(
            int present,
            int halfDay,
            int absent,
            int late,
            int wfh,
            int holidays,
            int weeklyOffs,
            int leaves,
            int workedMinutes,
            double payableDays) {
    }

    public record MonthView(String month, String userName, List<DayView> days, Totals totals) {
    }

    public record TodayView(
            LocalDate date,
            String dayType,
            String holidayName,
            boolean checkedIn,
            boolean checkedOut,
            Instant checkIn,
            Instant checkOut,
            WorkMode workMode,
            boolean late,
            Integer workedMinutes,
            AttendanceStatus status,
            String shiftStart,
            String optionalHolidayName,
            LeaveKind leaveKind) {
    }

    public record TeamRow(
            UUID userId,
            String name,
            String email,
            String state,
            WorkMode workMode,
            Instant checkIn,
            Instant checkOut,
            Integer workedMinutes,
            boolean late,
            AttendanceSource source,
            String note) {
    }

    public record TeamDay(LocalDate date, String dayType, String holidayName, List<TeamRow> rows) {
    }

    public record MarkRequest(
            @NotNull UUID userId,
            @NotNull LocalDate workDate,
            @NotNull AttendanceStatus status,
            WorkMode workMode,
            @Pattern(regexp = TIME) String checkIn,
            @Pattern(regexp = TIME) String checkOut,
            @Size(max = 300) String note) {
    }

    public record SummaryRow(UUID userId, String name, String email, Totals totals) {
    }

    public record CorrectionRequest(
            @NotNull LocalDate workDate,
            @NotBlank @Pattern(regexp = TIME) String checkIn,
            @Pattern(regexp = TIME) String checkOut,
            @NotNull WorkMode workMode,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record DecisionRequest(@Size(max = 300) String note, Long version) {
    }

    public record CorrectionView(
            UUID id,
            UUID userId,
            String userName,
            LocalDate workDate,
            String checkIn,
            String checkOut,
            WorkMode workMode,
            String reason,
            RequestStatus status,
            String decidedBy,
            Instant decidedAt,
            String decisionNote,
            Instant createdAt,
            Long version) {
    }

    public record HolidayEntry(
            UUID id,
            LocalDate date,
            String name,
            HolidayKind kind,
            boolean claimed,
            UUID leaveId,
            String leaveStatus) {
    }

    public record CompOffView(
            UUID id,
            LocalDate earnedDate,
            LocalDate expiresOn,
            String status,
            LocalDate usedOn,
            String source,
            String note) {
    }

    public record LeaveView(UUID id, LocalDate date, LeaveKind kind, String label, LeaveStatus status) {
    }

    public record LeaveOverview(
            int year,
            int optionalQuota,
            int optionalUsed,
            int compOffAvailable,
            List<HolidayEntry> holidays,
            List<CompOffView> compOffs,
            List<LeaveView> leaves) {
    }

    public record LeaveRow(
            UUID id,
            UUID userId,
            String userName,
            LocalDate date,
            LeaveKind kind,
            String label,
            LeaveStatus status,
            String decidedBy,
            String decisionNote,
            Instant createdAt) {
    }

    public record ClaimRequest(@NotNull UUID holidayId) {
    }

    public record UseCompOffRequest(@NotNull LocalDate date) {
    }

    public record GrantCompOffRequest(
            @NotNull UUID userId,
            @NotNull LocalDate earnedDate,
            @Size(max = 300) String note) {
    }
}
