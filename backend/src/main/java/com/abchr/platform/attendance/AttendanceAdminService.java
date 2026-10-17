package com.abchr.platform.attendance;

import com.abchr.platform.attendance.AttendanceDtos.DayView;
import com.abchr.platform.attendance.AttendanceDtos.MarkRequest;
import com.abchr.platform.attendance.AttendanceDtos.SummaryRow;
import com.abchr.platform.attendance.AttendanceDtos.TeamDay;
import com.abchr.platform.attendance.AttendanceDtos.TeamRow;
import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceAdminService {

    private final AttendanceRepository repository;
    private final AttendanceSettingsService settingsService;
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final LeaveService leaveService;
    private final ZoneId zone;

    public AttendanceAdminService(
            AttendanceRepository repository,
            AttendanceSettingsService settingsService,
            AttendanceService attendanceService,
            UserRepository userRepository,
            AuditService auditService,
            LeaveService leaveService,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.repository = repository;
        this.settingsService = settingsService;
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.leaveService = leaveService;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public TeamDay team(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now(zone) : date;
        LocalDate today = LocalDate.now(zone);
        Map<UUID, Map<LocalDate, LeaveDay>> allLeaves = leaveService.leaveDaysForAll(day, day);
        AttendanceCalendar.Context base = settingsService.context(day, day, Map.of());
        String type = AttendanceCalendar.dayType(day, base);
        Map<UUID, Attendance> records = new HashMap<>();
        for (Attendance a : repository.findByWorkDate(day)) {
            records.put(a.getUserId(), a);
        }
        List<TeamRow> rows = activeUsers().stream()
                .map(user -> {
                    Attendance a = records.get(user.getId());
                    AttendanceCalendar.Context context = base.withLeaves(allLeaves.getOrDefault(user.getId(), Map.of()));
                    return new TeamRow(user.getId(), user.getFullName(), user.getEmail(),
                            AttendanceCalendar.state(a, context, day, today),
                            a == null ? null : a.getWorkMode(),
                            a == null ? null : a.getCheckIn(),
                            a == null ? null : a.getCheckOut(),
                            a == null ? null : a.getWorkedMinutes(),
                            a != null && a.isLate(),
                            a == null ? null : a.getSource(),
                            a == null ? null : a.getNote());
                })
                .toList();
        return new TeamDay(day, type, base.holidays().get(day), rows);
    }

    @Transactional
    public void mark(MarkRequest request) {
        LocalDate today = LocalDate.now(zone);
        if (request.workDate().isAfter(today)) {
            throw ApiException.rule(ErrorCodes.ATTENDANCE_DATE_INVALID, "Cannot mark attendance for a future date");
        }
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.ATTENDANCE_USER_NOT_FOUND, "User not found"));
        LocalTime in = request.checkIn() == null ? null : LocalTime.parse(request.checkIn());
        LocalTime out = request.checkOut() == null ? null : LocalTime.parse(request.checkOut());
        if (out != null && (in == null || !out.isAfter(in))) {
            throw ApiException.rule(ErrorCodes.ATTENDANCE_TIME_INVALID, "Check-out must be after check-in");
        }
        Attendance record = repository.findByUserIdAndWorkDate(request.userId(), request.workDate())
                .orElseGet(() -> Attendance.create(request.userId(), request.workDate()));
        AttendanceSettings settings = settingsService.load();
        boolean absent = request.status() == AttendanceStatus.ABSENT;
        record.checkIn = absent || in == null ? null : AttendanceCalendar.at(request.workDate(), in, zone);
        record.checkOut = absent || out == null ? null : AttendanceCalendar.at(request.workDate(), out, zone);
        record.workMode = request.workMode() == null ? WorkMode.OFFICE : request.workMode();
        record.source = AttendanceSource.HR;
        record.note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        record.evaluate(settings, zone);
        record.status = request.status();
        if (absent) {
            record.late = false;
            record.workedMinutes = null;
        }
        repository.saveAndFlush(record);
        leaveService.syncCompOff(record);
        auditService.record(AuditActions.ATTENDANCE_MARKED, AuditActions.ENTITY_ATTENDANCE,
                record.getId().toString(),
                "Marked " + user.getFullName() + " " + request.status() + " on " + request.workDate(), null);
    }

    @Transactional(readOnly = true)
    public List<SummaryRow> summary(String month) {
        YearMonth ym = attendanceService.parse(month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        LocalDate today = LocalDate.now(zone);
        AttendanceCalendar.Context base = settingsService.context(from, to, Map.of());
        Map<UUID, Map<LocalDate, LeaveDay>> allLeaves = leaveService.leaveDaysForAll(from, to);
        Map<UUID, Map<LocalDate, Attendance>> byUser = new HashMap<>();
        for (Attendance a : repository.findByWorkDateBetween(from, to)) {
            byUser.computeIfAbsent(a.getUserId(), k -> new HashMap<>()).put(a.getWorkDate(), a);
        }
        return activeUsers().stream()
                .map(user -> {
                    List<DayView> days = AttendanceCalendar.month(ym, today,
                            byUser.getOrDefault(user.getId(), Map.of()),
                            base.withLeaves(allLeaves.getOrDefault(user.getId(), Map.of())), new HashSet<>());
                    return new SummaryRow(user.getId(), user.getFullName(), user.getEmail(),
                            AttendanceCalendar.totals(days));
                })
                .toList();
    }

    private List<AppUser> activeUsers() {
        return userRepository.findAll().stream()
                .filter(AppUser::isActive)
                .sorted(Comparator.comparing(u -> u.getFullName().toLowerCase()))
                .toList();
    }
}
