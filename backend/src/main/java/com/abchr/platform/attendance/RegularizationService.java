package com.abchr.platform.attendance;

import com.abchr.platform.attendance.AttendanceDtos.CorrectionRequest;
import com.abchr.platform.attendance.AttendanceDtos.CorrectionView;
import com.abchr.platform.attendance.AttendanceDtos.DecisionRequest;
import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegularizationService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int WINDOW_DAYS = 31;

    private final AttendanceRequestRepository requestRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSettingsService settingsService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final LeaveService leaveService;
    private final ZoneId zone;

    public RegularizationService(
            AttendanceRequestRepository requestRepository,
            AttendanceRepository attendanceRepository,
            AttendanceSettingsService settingsService,
            UserRepository userRepository,
            AuditService auditService,
            LeaveService leaveService,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.requestRepository = requestRepository;
        this.attendanceRepository = attendanceRepository;
        this.settingsService = settingsService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.leaveService = leaveService;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional
    public CorrectionView create(CorrectionRequest request) {
        UUID userId = AttendanceService.currentUserId();
        LocalDate today = LocalDate.now(zone);
        if (request.workDate().isAfter(today) || request.workDate().isBefore(today.minusDays(WINDOW_DAYS))) {
            throw ApiException.rule(ErrorCodes.ATTENDANCE_DATE_INVALID,
                    "Date must be within the last " + WINDOW_DAYS + " days and not in the future");
        }
        LocalTime in = LocalTime.parse(request.checkIn());
        LocalTime out = request.checkOut() == null ? null : LocalTime.parse(request.checkOut());
        if (out != null && !out.isAfter(in)) {
            throw ApiException.rule(ErrorCodes.ATTENDANCE_TIME_INVALID, "Check-out must be after check-in");
        }
        if (requestRepository.existsByUserIdAndWorkDateAndStatus(userId, request.workDate(), RequestStatus.PENDING)) {
            throw ApiException.conflict(ErrorCodes.ATTENDANCE_REQUEST_EXISTS,
                    "A correction for this date is already waiting for approval");
        }
        AttendanceRequest entity = AttendanceRequest.create();
        entity.userId = userId;
        entity.workDate = request.workDate();
        entity.checkIn = in;
        entity.checkOut = out;
        entity.workMode = request.workMode();
        entity.reason = request.reason().trim();
        requestRepository.saveAndFlush(entity);
        auditService.record(AuditActions.ATTENDANCE_REQUEST_CREATED, AuditActions.ENTITY_ATTENDANCE_REQUEST,
                entity.getId().toString(), "Requested attendance correction for " + entity.workDate, null);
        return toView(entity, name(userId));
    }

    @Transactional(readOnly = true)
    public List<CorrectionView> mine() {
        UUID userId = AttendanceService.currentUserId();
        String name = name(userId);
        return requestRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100)).stream()
                .map(r -> toView(r, name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CorrectionView> list(RequestStatus status) {
        PageRequest page = PageRequest.of(0, 200);
        List<AttendanceRequest> rows = status == null
                ? requestRepository.findAllByOrderByCreatedAtDesc(page)
                : requestRepository.findByStatusOrderByCreatedAtDesc(status, page);
        Map<UUID, String> names = new HashMap<>();
        return rows.stream()
                .map(r -> toView(r, names.computeIfAbsent(r.userId, this::name)))
                .toList();
    }

    @Transactional
    public CorrectionView approve(UUID id, DecisionRequest request) {
        AttendanceRequest entity = open(id, request);
        AttendanceSettings settings = settingsService.load();
        Attendance record = attendanceRepository.findByUserIdAndWorkDate(entity.userId, entity.workDate)
                .orElseGet(() -> Attendance.create(entity.userId, entity.workDate));
        record.checkIn = AttendanceCalendar.at(entity.workDate, entity.checkIn, zone);
        record.checkOut = entity.checkOut == null ? null : AttendanceCalendar.at(entity.workDate, entity.checkOut, zone);
        record.workMode = entity.workMode;
        record.source = AttendanceSource.REQUEST;
        record.note = "Corrected: " + entity.reason;
        record.status = AttendanceStatus.PRESENT;
        record.evaluate(settings, zone);
        attendanceRepository.saveAndFlush(record);
        leaveService.syncCompOff(record);
        decide(entity, RequestStatus.APPROVED, request);
        auditService.record(AuditActions.ATTENDANCE_REQUEST_APPROVED, AuditActions.ENTITY_ATTENDANCE_REQUEST,
                id.toString(), "Approved attendance correction for " + entity.workDate, null);
        return toView(entity, name(entity.userId));
    }

    @Transactional
    public CorrectionView reject(UUID id, DecisionRequest request) {
        AttendanceRequest entity = open(id, request);
        decide(entity, RequestStatus.REJECTED, request);
        auditService.record(AuditActions.ATTENDANCE_REQUEST_REJECTED, AuditActions.ENTITY_ATTENDANCE_REQUEST,
                id.toString(), "Rejected attendance correction for " + entity.workDate, null);
        return toView(entity, name(entity.userId));
    }

    private AttendanceRequest open(UUID id, DecisionRequest request) {
        AttendanceRequest entity = requestRepository.findById(id).orElseThrow(
                () -> ApiException.notFound(ErrorCodes.ATTENDANCE_REQUEST_NOT_FOUND, "Request not found"));
        VersionCheck.verify(request == null ? null : request.version(), entity.getVersion(),
                AttendanceRequest.class, id);
        if (entity.status != RequestStatus.PENDING) {
            throw ApiException.conflict(ErrorCodes.ATTENDANCE_REQUEST_CLOSED, "This request is already decided");
        }
        return entity;
    }

    private void decide(AttendanceRequest entity, RequestStatus status, DecisionRequest request) {
        entity.status = status;
        entity.decidedBy = Actor.current().email();
        entity.decidedAt = Instant.now();
        entity.decisionNote = request == null || request.note() == null || request.note().isBlank()
                ? null : request.note().trim();
        requestRepository.saveAndFlush(entity);
    }

    private String name(UUID userId) {
        return userRepository.findById(userId).map(AppUser::getFullName).orElse("Unknown");
    }

    private static CorrectionView toView(AttendanceRequest r, String userName) {
        return new CorrectionView(r.getId(), r.userId, userName, r.workDate, r.checkIn.format(HHMM),
                r.checkOut == null ? null : r.checkOut.format(HHMM), r.workMode, r.reason, r.status, r.decidedBy,
                r.decidedAt, r.decisionNote, r.getCreatedAt(), r.getVersion());
    }
}
