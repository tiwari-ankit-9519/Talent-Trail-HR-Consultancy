package com.abchr.platform.attendance;

import com.abchr.platform.attendance.AttendanceDtos.ApplicationView;
import com.abchr.platform.attendance.AttendanceDtos.ApplyRequest;
import com.abchr.platform.attendance.AttendanceDtos.BalanceView;
import com.abchr.platform.attendance.AttendanceDtos.PreviewView;
import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveApplicationService {

    private static final List<LeaveStatus> ACTIVE = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveApplicationRepository applicationRepository;
    private final LeaveTypeRepository typeRepository;
    private final LeaveTypeService typeService;
    private final LeaveEntryRepository entryRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSettingsService settingsService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AttendanceNotifier notifier;
    private final ZoneId zone;

    public LeaveApplicationService(
            LeaveApplicationRepository applicationRepository,
            LeaveTypeRepository typeRepository,
            LeaveTypeService typeService,
            LeaveEntryRepository entryRepository,
            AttendanceRepository attendanceRepository,
            AttendanceSettingsService settingsService,
            UserRepository userRepository,
            AuditService auditService,
            AttendanceNotifier notifier,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.applicationRepository = applicationRepository;
        this.typeRepository = typeRepository;
        this.typeService = typeService;
        this.entryRepository = entryRepository;
        this.attendanceRepository = attendanceRepository;
        this.settingsService = settingsService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notifier = notifier;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public List<BalanceView> balances(Integer yearParam) {
        UUID userId = AttendanceService.currentUserId();
        int year = yearParam == null ? LocalDate.now(zone).getYear() : yearParam;
        List<BalanceView> out = new ArrayList<>();
        for (LeaveType type : typeRepository.findByActiveTrueOrderBySortOrderAscNameAsc()) {
            double used = used(userId, type.getId(), year, null);
            double yearly = type.yearlyDays.doubleValue();
            out.add(new BalanceView(type.getId(), type.name, type.unpaid, yearly, used,
                    type.unpaid ? 0 : Math.max(0, yearly - used)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public PreviewView preview(UUID leaveTypeId, LocalDate from, LocalDate to, HalfDay halfDay) {
        UUID userId = AttendanceService.currentUserId();
        LeaveType type = typeService.find(leaveTypeId);
        double days = countDays(from, to, halfDay);
        if (type.unpaid) {
            return new PreviewView(days, null, true, true);
        }
        double remaining = type.yearlyDays.doubleValue() - used(userId, leaveTypeId, from.getYear(), null);
        return new PreviewView(days, Math.max(0, remaining), false, remaining >= days);
    }

    @Transactional
    public ApplicationView apply(ApplyRequest request) {
        UUID userId = AttendanceService.currentUserId();
        LeaveType type = typeService.find(request.leaveTypeId());
        if (!type.active) {
            throw ApiException.rule(ErrorCodes.LEAVE_TYPE_INACTIVE, "This leave type is not available");
        }
        LocalDate from = request.fromDate();
        LocalDate to = request.toDate();
        double days = countDays(from, to, request.halfDay());
        if (!type.unpaid) {
            double remaining = type.yearlyDays.doubleValue() - used(userId, type.getId(), from.getYear(), null);
            if (remaining < days) {
                throw ApiException.rule(ErrorCodes.LEAVE_BALANCE_LOW,
                        "Only " + remaining + " day(s) of " + type.name + " left. Choose Loss of pay for the rest.");
            }
        }
        ensureFree(userId, from, to);
        LeaveApplication application = LeaveApplication.create();
        application.userId = userId;
        application.leaveTypeId = type.getId();
        application.typeName = type.name;
        application.unpaid = type.unpaid;
        application.fromDate = from;
        application.toDate = to;
        application.halfDay = request.halfDay();
        application.days = BigDecimal.valueOf(days);
        application.reason = request.reason().trim();
        applicationRepository.saveAndFlush(application);
        auditService.record(AuditActions.LEAVE_APPLIED, AuditActions.ENTITY_LEAVE_APPLICATION,
                application.getId().toString(),
                "Applied " + days + " day(s) of " + type.name + " from " + from + " to " + to, null);
        notifier.leaveRequested(userId, type.name + ", " + days + " day(s) from " + from + " to " + to);
        return toView(application, name(userId));
    }

    @Transactional(readOnly = true)
    public List<ApplicationView> mine() {
        UUID userId = AttendanceService.currentUserId();
        String name = name(userId);
        return applicationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100)).stream()
                .map(a -> toView(a, name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationView> requests(LeaveStatus status) {
        PageRequest page = PageRequest.of(0, 200);
        List<LeaveApplication> rows = status == null
                ? applicationRepository.findAllByOrderByCreatedAtDesc(page)
                : applicationRepository.findByStatusOrderByCreatedAtDesc(status, page);
        Map<UUID, String> names = new HashMap<>();
        return rows.stream().map(a -> toView(a, names.computeIfAbsent(a.userId, this::name))).toList();
    }

    @Transactional
    public void cancel(UUID id) {
        LeaveApplication application = find(id);
        boolean manager = Permissions.hasAny("ATTENDANCE_MANAGE", "ATTENDANCE_APPROVE");
        boolean owner = application.userId.equals(AttendanceService.currentUserId());
        if (!manager && !owner) {
            throw ApiException.forbidden(ErrorCodes.LEAVE_NOT_ALLOWED, "You cannot cancel someone else's leave");
        }
        if (!ACTIVE.contains(application.status)) {
            throw ApiException.conflict(ErrorCodes.LEAVE_CLOSED, "This leave is already closed");
        }
        if (!manager && application.fromDate.isBefore(LocalDate.now(zone))) {
            throw ApiException.rule(ErrorCodes.LEAVE_DATE_INVALID, "Leave that has started can only be cancelled by a manager");
        }
        application.status = LeaveStatus.CANCELLED;
        applicationRepository.saveAndFlush(application);
        auditService.record(AuditActions.LEAVE_APPLICATION_CANCELLED, AuditActions.ENTITY_LEAVE_APPLICATION,
                id.toString(), "Cancelled " + application.typeName + " from " + application.fromDate, null);
        notifier.leaveCancelled(application.userId, summary(application));
    }

    @Transactional
    public void approve(UUID id, String note) {
        LeaveApplication application = pendingForDecision(id);
        decide(application, LeaveStatus.APPROVED, note);
        auditService.record(AuditActions.LEAVE_APPLICATION_APPROVED, AuditActions.ENTITY_LEAVE_APPLICATION,
                id.toString(), "Approved " + application.typeName + " from " + application.fromDate + " for "
                        + name(application.userId), null);
        notifier.leaveDecided(application.userId, true, summary(application));
    }

    @Transactional
    public void reject(UUID id, String note) {
        LeaveApplication application = pendingForDecision(id);
        decide(application, LeaveStatus.REJECTED, note);
        auditService.record(AuditActions.LEAVE_APPLICATION_REJECTED, AuditActions.ENTITY_LEAVE_APPLICATION,
                id.toString(), "Rejected " + application.typeName + " from " + application.fromDate + " for "
                        + name(application.userId), null);
        notifier.leaveDecided(application.userId, false, summary(application));
    }

    private static String summary(LeaveApplication application) {
        return application.typeName + ", " + application.days + " day(s) from " + application.fromDate + " to "
                + application.toDate;
    }

    private double countDays(LocalDate from, LocalDate to, HalfDay halfDay) {
        if (to.isBefore(from)) {
            throw ApiException.rule(ErrorCodes.LEAVE_DATE_INVALID, "End date is before start date");
        }
        if (from.isBefore(LocalDate.now(zone).minusDays(31))) {
            throw ApiException.rule(ErrorCodes.LEAVE_DATE_INVALID, "Leave cannot start more than 31 days in the past");
        }
        if (to.isAfter(from.plusDays(120))) {
            throw ApiException.rule(ErrorCodes.LEAVE_DATE_INVALID, "Leave range is too long");
        }
        if (halfDay != null && !from.equals(to)) {
            throw ApiException.rule(ErrorCodes.LEAVE_HALF_DAY_RANGE, "Half day applies to a single date");
        }
        AttendanceCalendar.Context context = settingsService.context(from, to, Map.of());
        int working = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if ("WORKING".equals(AttendanceCalendar.dayType(day, context))) {
                working++;
            }
        }
        if (working == 0) {
            throw ApiException.rule(ErrorCodes.LEAVE_NO_WORKING_DAYS, "No working days in this range");
        }
        return halfDay != null ? 0.5 : working;
    }

    private double used(UUID userId, UUID typeId, int year, UUID excluding) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        double sum = 0;
        for (LeaveApplication a : applicationRepository.findByUserIdAndLeaveTypeIdAndStatusInAndFromDateBetween(
                userId, typeId, ACTIVE, from, to)) {
            if (!a.getId().equals(excluding)) {
                sum += a.days.doubleValue();
            }
        }
        return sum;
    }

    private void ensureFree(UUID userId, LocalDate from, LocalDate to) {
        if (!applicationRepository.findByUserIdAndStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                userId, ACTIVE, to, from).isEmpty()) {
            throw ApiException.conflict(ErrorCodes.LEAVE_ALREADY_TAKEN, "You already have leave in this range");
        }
        if (!entryRepository.findByUserIdAndLeaveDateBetweenAndStatusInOrderByLeaveDate(userId, from, to, ACTIVE)
                .isEmpty()) {
            throw ApiException.conflict(ErrorCodes.LEAVE_ALREADY_TAKEN,
                    "You already have an optional holiday or comp off in this range");
        }
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (attendanceRepository.findByUserIdAndWorkDate(userId, day)
                    .filter(a -> a.getCheckIn() != null).isPresent()) {
                throw ApiException.conflict(ErrorCodes.LEAVE_ALREADY_TAKEN, "You already have attendance on " + day);
            }
        }
    }

    private LeaveApplication find(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.LEAVE_NOT_FOUND, "Leave not found"));
    }

    private LeaveApplication pendingForDecision(UUID id) {
        LeaveApplication application = find(id);
        if (application.status != LeaveStatus.PENDING) {
            throw ApiException.conflict(ErrorCodes.LEAVE_CLOSED, "This request is already decided");
        }
        if (application.userId.equals(AttendanceService.currentUserId())) {
            throw ApiException.forbidden(ErrorCodes.LEAVE_SELF_APPROVAL, "You cannot decide your own leave request");
        }
        return application;
    }

    private void decide(LeaveApplication application, LeaveStatus status, String note) {
        application.status = status;
        application.decidedBy = Actor.current().email();
        application.decidedAt = Instant.now();
        application.decisionNote = note == null || note.isBlank() ? null : note.trim();
        applicationRepository.saveAndFlush(application);
    }

    private String name(UUID userId) {
        return userRepository.findById(userId).map(AppUser::getFullName).orElse("Unknown");
    }

    private static ApplicationView toView(LeaveApplication a, String userName) {
        return new ApplicationView(a.getId(), a.userId, userName, a.leaveTypeId, a.typeName, a.unpaid, a.fromDate,
                a.toDate, a.halfDay, a.days.doubleValue(), a.reason, a.status, a.decidedBy, a.decisionNote,
                a.getCreatedAt(), a.getVersion());
    }
}
