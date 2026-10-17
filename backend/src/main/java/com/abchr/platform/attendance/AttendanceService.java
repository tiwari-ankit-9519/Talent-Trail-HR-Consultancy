package com.abchr.platform.attendance;

import com.abchr.platform.attendance.AttendanceDtos.CheckInRequest;
import com.abchr.platform.attendance.AttendanceDtos.DayView;
import com.abchr.platform.attendance.AttendanceDtos.MonthView;
import com.abchr.platform.attendance.AttendanceDtos.TodayView;
import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private final AttendanceRepository repository;
    private final AttendanceRequestRepository requestRepository;
    private final AttendanceSettingsService settingsService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final LeaveService leaveService;
    private final ZoneId zone;

    public AttendanceService(
            AttendanceRepository repository,
            AttendanceRequestRepository requestRepository,
            AttendanceSettingsService settingsService,
            UserRepository userRepository,
            AuditService auditService,
            LeaveService leaveService,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.repository = repository;
        this.requestRepository = requestRepository;
        this.settingsService = settingsService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.leaveService = leaveService;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public TodayView today() {
        UUID userId = currentUserId();
        LocalDate today = LocalDate.now(zone);
        AttendanceSettings settings = settingsService.load();
        AttendanceCalendar.Context context = settingsService.context(today, today,
                leaveService.leaveDaysFor(userId, today, today));
        String type = AttendanceCalendar.dayType(today, context);
        Attendance record = repository.findByUserIdAndWorkDate(userId, today).orElse(null);
        String shift = settings.getShiftStart().toString().substring(0, 5);
        LeaveDay leave = context.leaves().get(today);
        String leaveLabel = leave == null ? null : leave.label();
        String optional = context.optional().get(today);
        if (record == null) {
            return new TodayView(today, type, context.holidays().get(today), false, false, null, null, null, false,
                    null, null, shift, optional, leaveLabel);
        }
        return new TodayView(today, type, context.holidays().get(today), record.getCheckIn() != null,
                record.getCheckOut() != null, record.getCheckIn(), record.getCheckOut(), record.getWorkMode(),
                record.isLate(), record.getWorkedMinutes(), record.getStatus(), shift, optional, leaveLabel);
    }

    @Transactional
    public TodayView checkIn(CheckInRequest request) {
        UUID userId = currentUserId();
        LocalDate today = LocalDate.now(zone);
        AttendanceSettings settings = settingsService.load();
        Attendance record = repository.findByUserIdAndWorkDate(userId, today)
                .orElseGet(() -> Attendance.create(userId, today));
        if (record.checkIn != null) {
            throw ApiException.conflict(ErrorCodes.ATTENDANCE_ALREADY_CHECKED_IN, "You have already checked in today");
        }
        record.checkIn = Instant.now();
        record.checkOut = null;
        record.workMode = request != null && request.workMode() != null ? request.workMode() : WorkMode.OFFICE;
        record.source = AttendanceSource.SELF;
        record.note = request == null || request.note() == null || request.note().isBlank()
                ? null : request.note().trim();
        record.status = AttendanceStatus.PRESENT;
        record.evaluate(settings, zone);
        repository.saveAndFlush(record);
        auditService.record(AuditActions.ATTENDANCE_CHECK_IN, AuditActions.ENTITY_ATTENDANCE,
                record.getId().toString(), "Checked in" + (record.isLate() ? " (late)" : ""), null);
        return today();
    }

    @Transactional
    public TodayView checkOut() {
        UUID userId = currentUserId();
        LocalDate today = LocalDate.now(zone);
        AttendanceSettings settings = settingsService.load();
        Attendance record = repository.findByUserIdAndWorkDate(userId, today)
                .filter(r -> r.checkIn != null)
                .orElseThrow(() -> ApiException.rule(ErrorCodes.ATTENDANCE_NOT_CHECKED_IN,
                        "You have not checked in today"));
        if (record.checkOut != null) {
            throw ApiException.conflict(ErrorCodes.ATTENDANCE_ALREADY_CHECKED_OUT,
                    "You have already checked out today");
        }
        record.checkOut = Instant.now();
        record.evaluate(settings, zone);
        repository.saveAndFlush(record);
        leaveService.syncCompOff(record);
        auditService.record(AuditActions.ATTENDANCE_CHECK_OUT, AuditActions.ENTITY_ATTENDANCE,
                record.getId().toString(), "Checked out after " + record.getWorkedMinutes() + " minutes", null);
        return today();
    }

    @Transactional(readOnly = true)
    public MonthView month(String month) {
        return monthFor(currentUserId(), month);
    }

    @Transactional(readOnly = true)
    public MonthView monthFor(UUID userId, String month) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.ATTENDANCE_USER_NOT_FOUND, "User not found"));
        YearMonth ym = parse(month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        Map<LocalDate, Attendance> records = new HashMap<>();
        for (Attendance a : repository.findByUserIdAndWorkDateBetween(userId, from, to)) {
            records.put(a.getWorkDate(), a);
        }
        Set<LocalDate> pending = new HashSet<>();
        for (AttendanceRequest r : requestRepository.findByUserIdAndWorkDateBetweenAndStatus(
                userId, from, to, RequestStatus.PENDING)) {
            pending.add(r.workDate);
        }
        pending.addAll(leaveService.pendingDates(userId, from, to));
        AttendanceCalendar.Context context = settingsService.context(from, to,
                leaveService.leaveDaysFor(userId, from, to));
        java.util.List<DayView> days = AttendanceCalendar.month(ym, LocalDate.now(zone), records, context, pending);
        return new MonthView(ym.toString(), user.getFullName(), days, AttendanceCalendar.totals(days));
    }

    @Transactional(readOnly = true)
    public AttendanceDtos.Totals totalsFrom(UUID userId, String month, LocalDate fromDate) {
        MonthView view = monthFor(userId, month);
        return AttendanceCalendar.totals(view.days().stream().filter(d -> !d.date().isBefore(fromDate)).toList());
    }

    YearMonth parse(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(zone);
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            throw ApiException.rule(ErrorCodes.ATTENDANCE_MONTH_INVALID, "Month must look like 2026-10");
        }
    }

    static UUID currentUserId() {
        UUID id = Actor.current().id();
        if (id == null) {
            throw ApiException.forbidden(ErrorCodes.ATTENDANCE_USER_NOT_FOUND, "No signed-in user");
        }
        return id;
    }
}
