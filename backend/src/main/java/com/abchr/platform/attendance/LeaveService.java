package com.abchr.platform.attendance;

import com.abchr.platform.attendance.AttendanceDtos.CompOffView;
import com.abchr.platform.attendance.AttendanceDtos.GrantCompOffRequest;
import com.abchr.platform.attendance.AttendanceDtos.HolidayEntry;
import com.abchr.platform.attendance.AttendanceDtos.LeaveOverview;
import com.abchr.platform.attendance.AttendanceDtos.LeaveRow;
import com.abchr.platform.attendance.AttendanceDtos.LeaveView;
import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveService {

    static final List<LeaveStatus> ACTIVE = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveEntryRepository leaveRepository;
    private final LeaveApplicationRepository applicationRepository;
    private final CompOffRepository compOffRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSettingsService settingsService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AttendanceNotifier notifier;
    private final ZoneId zone;

    public LeaveService(
            LeaveEntryRepository leaveRepository,
            LeaveApplicationRepository applicationRepository,
            CompOffRepository compOffRepository,
            AttendanceRepository attendanceRepository,
            AttendanceSettingsService settingsService,
            UserRepository userRepository,
            AuditService auditService,
            AttendanceNotifier notifier,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.leaveRepository = leaveRepository;
        this.applicationRepository = applicationRepository;
        this.compOffRepository = compOffRepository;
        this.attendanceRepository = attendanceRepository;
        this.settingsService = settingsService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notifier = notifier;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, LeaveDay> leaveDaysFor(UUID userId, LocalDate from, LocalDate to) {
        Map<LocalDate, LeaveDay> map = new HashMap<>();
        for (LeaveEntry entry : leaveRepository.findByUserIdAndLeaveDateBetweenAndStatusInOrderByLeaveDate(
                userId, from, to, List.of(LeaveStatus.APPROVED))) {
            map.put(entry.getLeaveDate(), new LeaveDay(label(entry), 1, false));
        }
        for (LeaveApplication application : applicationRepository
                .findByUserIdAndStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        userId, List.of(LeaveStatus.APPROVED), to, from)) {
            addDays(map, application, from, to);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<UUID, Map<LocalDate, LeaveDay>> leaveDaysForAll(LocalDate from, LocalDate to) {
        Map<UUID, Map<LocalDate, LeaveDay>> map = new HashMap<>();
        for (LeaveEntry entry : leaveRepository.findByLeaveDateBetweenAndStatusOrderByLeaveDate(
                from, to, LeaveStatus.APPROVED)) {
            map.computeIfAbsent(entry.getUserId(), k -> new HashMap<>())
                    .put(entry.getLeaveDate(), new LeaveDay(label(entry), 1, false));
        }
        for (LeaveApplication application : applicationRepository
                .findByStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        List.of(LeaveStatus.APPROVED), to, from)) {
            addDays(map.computeIfAbsent(application.userId, k -> new HashMap<>()), application, from, to);
        }
        return map;
    }

    private static void addDays(Map<LocalDate, LeaveDay> target, LeaveApplication application, LocalDate from,
            LocalDate to) {
        double fraction = application.halfDay == null ? 1 : 0.5;
        LocalDate day = application.fromDate.isBefore(from) ? from : application.fromDate;
        LocalDate end = application.toDate.isAfter(to) ? to : application.toDate;
        for (; !day.isAfter(end); day = day.plusDays(1)) {
            target.put(day, new LeaveDay(application.typeName, fraction, application.unpaid));
        }
    }

    @Transactional(readOnly = true)
    public java.util.Set<LocalDate> pendingDates(UUID userId, LocalDate from, LocalDate to) {
        java.util.Set<LocalDate> dates = new java.util.HashSet<>();
        for (LeaveEntry entry : leaveRepository.findByUserIdAndLeaveDateBetweenAndStatusInOrderByLeaveDate(
                userId, from, to, List.of(LeaveStatus.PENDING))) {
            dates.add(entry.getLeaveDate());
        }
        for (LeaveApplication application : applicationRepository
                .findByUserIdAndStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        userId, List.of(LeaveStatus.PENDING), to, from)) {
            LocalDate day = application.fromDate.isBefore(from) ? from : application.fromDate;
            LocalDate end = application.toDate.isAfter(to) ? to : application.toDate;
            for (; !day.isAfter(end); day = day.plusDays(1)) {
                dates.add(day);
            }
        }
        return dates;
    }

    @Transactional(readOnly = true)
    public LeaveOverview overview(Integer yearParam) {
        UUID userId = AttendanceService.currentUserId();
        LocalDate today = LocalDate.now(zone);
        int year = yearParam == null ? today.getYear() : yearParam;
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        AttendanceSettings settings = settingsService.load();
        List<LeaveEntry> mine = leaveRepository.findByUserIdAndLeaveDateBetweenAndStatusInOrderByLeaveDate(
                userId, from, to, ACTIVE);
        Map<UUID, LeaveEntry> claimedByHoliday = new HashMap<>();
        for (LeaveEntry entry : mine) {
            if (entry.holidayId != null) {
                claimedByHoliday.put(entry.holidayId, entry);
            }
        }
        List<HolidayEntry> holidays = new ArrayList<>();
        for (Holiday holiday : settingsService.holidayRows(from, to)) {
            LeaveEntry claim = claimedByHoliday.get(holiday.getId());
            holidays.add(new HolidayEntry(holiday.getId(), holiday.getHolidayDate(), holiday.getName(),
                    holiday.getKind(), claim != null, claim == null ? null : claim.getId(),
                    claim == null ? null : claim.status.name()));
        }
        int optionalUsed = (int) mine.stream().filter(e -> e.getKind() == LeaveKind.OPTIONAL_HOLIDAY).count();
        List<CompOffView> credits = compOffRepository.findByUserIdOrderByEarnedDateDesc(userId).stream()
                .map(c -> toView(c, today))
                .toList();
        int available = (int) credits.stream().filter(c -> "AVAILABLE".equals(c.status())).count();
        List<LeaveView> leaves = mine.stream()
                .map(e -> new LeaveView(e.getId(), e.getLeaveDate(), e.getKind(), label(e), e.status))
                .toList();
        return new LeaveOverview(year, settings.getOptionalQuota(), optionalUsed, available, holidays, credits, leaves);
    }

    @Transactional
    public void claimOptional(UUID holidayId) {
        UUID userId = AttendanceService.currentUserId();
        LocalDate today = LocalDate.now(zone);
        Holiday holiday = settingsService.findHoliday(holidayId);
        if (holiday.getKind() != HolidayKind.OPTIONAL) {
            throw ApiException.rule(ErrorCodes.LEAVE_NOT_OPTIONAL_HOLIDAY, "This holiday is not optional");
        }
        LocalDate date = holiday.getHolidayDate();
        if (date.isBefore(today)) {
            throw ApiException.rule(ErrorCodes.LEAVE_DATE_INVALID, "That date has already passed");
        }
        ensureFree(userId, date);
        AttendanceSettings settings = settingsService.load();
        long used = leaveRepository.countByUserIdAndKindAndStatusInAndLeaveDateBetween(userId,
                LeaveKind.OPTIONAL_HOLIDAY, ACTIVE, LocalDate.of(date.getYear(), 1, 1),
                LocalDate.of(date.getYear(), 12, 31));
        if (used >= settings.getOptionalQuota()) {
            throw ApiException.rule(ErrorCodes.LEAVE_QUOTA_EXCEEDED,
                    "You have used all " + settings.getOptionalQuota() + " optional holidays for " + date.getYear());
        }
        LeaveEntry entry = leaveRepository.saveAndFlush(
                LeaveEntry.create(userId, date, LeaveKind.OPTIONAL_HOLIDAY, holidayId, null));
        auditService.record(AuditActions.LEAVE_OPTIONAL_CLAIMED, AuditActions.ENTITY_LEAVE,
                entry.getId().toString(), "Requested optional holiday " + holiday.getName() + " on " + date, null);
        notifier.leaveRequested(userId, "Optional holiday " + holiday.getName() + " on " + date);
    }

    @Transactional
    public void useCompOff(LocalDate date) {
        UUID userId = AttendanceService.currentUserId();
        LocalDate today = LocalDate.now(zone);
        if (date.isBefore(today)) {
            throw ApiException.rule(ErrorCodes.LEAVE_DATE_INVALID, "Comp off can only be taken from today onwards");
        }
        AttendanceSettings settings = settingsService.load();
        Map<LocalDate, String> holidays = settingsService.holidayMap(date, date);
        if (holidays.containsKey(date) || settings.weeklyOffDays().contains(date.getDayOfWeek())) {
            throw ApiException.rule(ErrorCodes.LEAVE_DATE_INVALID, "That day is already an off day");
        }
        ensureFree(userId, date);
        CompOff credit = compOffRepository
                .findByUserIdAndStatusAndExpiresOnGreaterThanEqualOrderByEarnedDate(
                        userId, CompOffStatus.AVAILABLE, date)
                .stream()
                .findFirst()
                .orElseThrow(() -> ApiException.rule(ErrorCodes.COMP_OFF_NONE,
                        "You have no comp off available for that date"));
        credit.status = CompOffStatus.USED;
        credit.usedOn = date;
        compOffRepository.saveAndFlush(credit);
        LeaveEntry entry = leaveRepository.saveAndFlush(
                LeaveEntry.create(userId, date, LeaveKind.COMP_OFF, null, credit.getId()));
        auditService.record(AuditActions.LEAVE_COMP_OFF_USED, AuditActions.ENTITY_LEAVE, entry.getId().toString(),
                "Requested comp off earned on " + credit.earnedDate + " for " + date, null);
        notifier.leaveRequested(userId, "Comp off on " + date);
    }

    @Transactional
    public void cancel(UUID id) {
        LeaveEntry entry = find(id);
        boolean manager = Permissions.hasAny("ATTENDANCE_MANAGE", "ATTENDANCE_APPROVE");
        if (!manager && !entry.getUserId().equals(AttendanceService.currentUserId())) {
            throw ApiException.forbidden(ErrorCodes.LEAVE_NOT_ALLOWED, "You cannot cancel someone else's leave");
        }
        if (!ACTIVE.contains(entry.status)) {
            throw ApiException.conflict(ErrorCodes.LEAVE_CLOSED, "This leave is already closed");
        }
        if (!manager && entry.getLeaveDate().isBefore(LocalDate.now(zone))) {
            throw ApiException.rule(ErrorCodes.LEAVE_DATE_INVALID, "Past leave can only be cancelled by a manager");
        }
        entry.status = LeaveStatus.CANCELLED;
        leaveRepository.saveAndFlush(entry);
        releaseCredit(entry);
        auditService.record(AuditActions.LEAVE_CANCELLED, AuditActions.ENTITY_LEAVE, id.toString(),
                "Cancelled " + entry.getKind() + " leave on " + entry.getLeaveDate(), null);
        notifier.leaveCancelled(entry.getUserId(), label(entry) + " on " + entry.getLeaveDate());
    }

    @Transactional
    public void approve(UUID id, String note) {
        LeaveEntry entry = pendingForDecision(id);
        decide(entry, LeaveStatus.APPROVED, note);
        auditService.record(AuditActions.LEAVE_APPROVED, AuditActions.ENTITY_LEAVE, id.toString(),
                "Approved " + entry.getKind() + " leave on " + entry.getLeaveDate() + " for " + name(entry.getUserId()),
                null);
        notifier.leaveDecided(entry.getUserId(), true, label(entry) + " on " + entry.getLeaveDate());
    }

    @Transactional
    public void reject(UUID id, String note) {
        LeaveEntry entry = pendingForDecision(id);
        decide(entry, LeaveStatus.REJECTED, note);
        releaseCredit(entry);
        auditService.record(AuditActions.LEAVE_REJECTED, AuditActions.ENTITY_LEAVE, id.toString(),
                "Rejected " + entry.getKind() + " leave on " + entry.getLeaveDate() + " for " + name(entry.getUserId()),
                null);
        notifier.leaveDecided(entry.getUserId(), false, label(entry) + " on " + entry.getLeaveDate());
    }

    @Transactional(readOnly = true)
    public List<LeaveRow> requests(LeaveStatus status) {
        org.springframework.data.domain.PageRequest page = org.springframework.data.domain.PageRequest.of(0, 200);
        List<LeaveEntry> rows = status == null
                ? leaveRepository.findAllByOrderByCreatedAtDesc(page)
                : leaveRepository.findByStatusOrderByCreatedAtDesc(status, page);
        Map<UUID, String> names = new HashMap<>();
        return rows.stream()
                .map(e -> new LeaveRow(e.getId(), e.getUserId(), names.computeIfAbsent(e.getUserId(), this::name),
                        e.getLeaveDate(), e.getKind(), label(e), e.status, e.decidedBy, e.decisionNote,
                        e.getCreatedAt()))
                .toList();
    }

    private LeaveEntry find(UUID id) {
        return leaveRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.LEAVE_NOT_FOUND, "Leave not found"));
    }

    private LeaveEntry pendingForDecision(UUID id) {
        LeaveEntry entry = find(id);
        if (entry.status != LeaveStatus.PENDING) {
            throw ApiException.conflict(ErrorCodes.LEAVE_CLOSED, "This request is already decided");
        }
        if (entry.getUserId().equals(AttendanceService.currentUserId())) {
            throw ApiException.forbidden(ErrorCodes.LEAVE_SELF_APPROVAL, "You cannot decide your own leave request");
        }
        return entry;
    }

    private void decide(LeaveEntry entry, LeaveStatus status, String note) {
        entry.status = status;
        entry.decidedBy = com.abchr.platform.shared.security.Actor.current().email();
        entry.decidedAt = java.time.Instant.now();
        entry.decisionNote = note == null || note.isBlank() ? null : note.trim();
        leaveRepository.saveAndFlush(entry);
    }

    private void releaseCredit(LeaveEntry entry) {
        if (entry.compOffId != null) {
            compOffRepository.findById(entry.compOffId).ifPresent(credit -> {
                credit.status = CompOffStatus.AVAILABLE;
                credit.usedOn = null;
                compOffRepository.saveAndFlush(credit);
            });
        }
    }

    @Transactional
    public void grantCompOff(GrantCompOffRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.ATTENDANCE_USER_NOT_FOUND, "User not found"));
        if (request.earnedDate().isAfter(LocalDate.now(zone))) {
            throw ApiException.rule(ErrorCodes.LEAVE_DATE_INVALID, "Earned date cannot be in the future");
        }
        if (compOffRepository.findByUserIdAndEarnedDate(user.getId(), request.earnedDate()).isPresent()) {
            throw ApiException.conflict(ErrorCodes.COMP_OFF_EXISTS, "A comp off already exists for that date");
        }
        AttendanceSettings settings = settingsService.load();
        CompOff credit = compOffRepository.saveAndFlush(CompOff.create(user.getId(), request.earnedDate(),
                request.earnedDate().plusDays(settings.getCompOffValidDays()), "HR",
                request.note() == null || request.note().isBlank() ? null : request.note().trim()));
        auditService.record(AuditActions.COMP_OFF_GRANTED, AuditActions.ENTITY_COMP_OFF, credit.getId().toString(),
                "Granted comp off to " + user.getFullName() + " for " + request.earnedDate(), null);
    }

    @Transactional
    public void syncCompOff(Attendance record) {
        LocalDate date = record.getWorkDate();
        AttendanceSettings settings = settingsService.load();
        boolean off = settingsService.holidayMap(date, date).containsKey(date)
                || settings.weeklyOffDays().contains(date.getDayOfWeek());
        boolean earns = off && record.getStatus() == AttendanceStatus.PRESENT
                && (record.getCheckOut() != null || record.getSource() == AttendanceSource.HR);
        CompOff existing = compOffRepository.findByUserIdAndEarnedDate(record.getUserId(), date).orElse(null);
        if (earns && existing == null) {
            CompOff credit = compOffRepository.saveAndFlush(CompOff.create(record.getUserId(), date,
                    date.plusDays(settings.getCompOffValidDays()), "AUTO", null));
            auditService.record(AuditActions.COMP_OFF_EARNED, AuditActions.ENTITY_COMP_OFF,
                    credit.getId().toString(), "Earned comp off for working on " + date, null);
        } else if (!earns && existing != null && "AUTO".equals(existing.source)
                && existing.status == CompOffStatus.AVAILABLE) {
            compOffRepository.delete(existing);
        }
    }

    private void ensureFree(UUID userId, LocalDate date) {
        if (leaveRepository.findByUserIdAndLeaveDateAndStatusIn(userId, date, ACTIVE).isPresent()) {
            throw ApiException.conflict(ErrorCodes.LEAVE_ALREADY_TAKEN, "You already have leave on " + date);
        }
        if (attendanceRepository.findByUserIdAndWorkDate(userId, date)
                .filter(a -> a.getCheckIn() != null).isPresent()) {
            throw ApiException.conflict(ErrorCodes.LEAVE_ALREADY_TAKEN, "You already have attendance on " + date);
        }
    }

    private String name(UUID userId) {
        return userRepository.findById(userId).map(AppUser::getFullName).orElse("Unknown");
    }

    private static String label(LeaveEntry entry) {
        return entry.getKind() == LeaveKind.COMP_OFF ? "Comp off" : "Optional holiday";
    }

    private static CompOffView toView(CompOff credit, LocalDate today) {
        String status = credit.status == CompOffStatus.USED
                ? "USED"
                : credit.expiresOn.isBefore(today) ? "EXPIRED" : "AVAILABLE";
        return new CompOffView(credit.getId(), credit.earnedDate, credit.expiresOn, status, credit.usedOn,
                credit.source, credit.note);
    }
}
