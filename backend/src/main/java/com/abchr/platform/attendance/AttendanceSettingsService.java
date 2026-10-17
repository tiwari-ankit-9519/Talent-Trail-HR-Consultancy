package com.abchr.platform.attendance;

import com.abchr.platform.attendance.AttendanceDtos.HolidayRequest;
import com.abchr.platform.attendance.AttendanceDtos.HolidayView;
import com.abchr.platform.attendance.AttendanceDtos.SettingsRequest;
import com.abchr.platform.attendance.AttendanceDtos.SettingsView;
import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceSettingsService {

    private final AttendanceSettingsRepository settingsRepository;
    private final HolidayRepository holidayRepository;
    private final AuditService auditService;
    private final LeaveEntryRepository leaveRepository;

    public AttendanceSettingsService(
            AttendanceSettingsRepository settingsRepository,
            HolidayRepository holidayRepository,
            AuditService auditService,
            LeaveEntryRepository leaveRepository) {
        this.settingsRepository = settingsRepository;
        this.holidayRepository = holidayRepository;
        this.auditService = auditService;
        this.leaveRepository = leaveRepository;
    }

    @Transactional(readOnly = true)
    public AttendanceSettings load() {
        return settingsRepository.findById((short) 1).orElseThrow();
    }

    @Transactional(readOnly = true)
    public SettingsView get() {
        return toView(load());
    }

    @Transactional
    public SettingsView update(SettingsRequest request) {
        AttendanceSettings settings = load();
        VersionCheck.verify(request.version(), settings.getVersion(), AttendanceSettings.class, 1);
        if (request.halfDayMinutes() >= request.fullDayMinutes()) {
            throw ApiException.rule(ErrorCodes.ATTENDANCE_SETTINGS_INVALID,
                    "Half day minutes must be lower than full day minutes");
        }
        Set<DayOfWeek> off = EnumSet.noneOf(DayOfWeek.class);
        for (String name : request.weeklyOff()) {
            try {
                off.add(DayOfWeek.valueOf(name.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw ApiException.rule(ErrorCodes.ATTENDANCE_SETTINGS_INVALID, "Unknown weekday " + name);
            }
        }
        LocalTime start = LocalTime.parse(request.shiftStart());
        Changes changes = Changes.create()
                .field("optionalQuota", settings.getOptionalQuota(), request.optionalQuota())
                .field("compOffValidDays", settings.getCompOffValidDays(), request.compOffValidDays())
                .field("shiftStart", settings.getShiftStart().toString(), start.toString())
                .field("lateGraceMinutes", settings.getLateGraceMinutes(), request.lateGraceMinutes())
                .field("halfDayMinutes", settings.getHalfDayMinutes(), request.halfDayMinutes())
                .field("fullDayMinutes", settings.getFullDayMinutes(), request.fullDayMinutes())
                .field("weeklyOff", settings.weeklyOffDays().toString(), off.toString());
        settings.apply(start, request.lateGraceMinutes(), request.halfDayMinutes(), request.fullDayMinutes(), off,
                request.optionalQuota(), request.compOffValidDays());
        settingsRepository.saveAndFlush(settings);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.ATTENDANCE_SETTINGS_UPDATED, AuditActions.ENTITY_ATTENDANCE_SETTINGS, "1",
                    changes.summary("Updated attendance rules"), changes.toJson());
        }
        return toView(settings);
    }

    @Transactional(readOnly = true)
    public List<HolidayView> holidays(int year) {
        return holidayRepository
                .findByHolidayDateBetweenOrderByHolidayDate(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .stream()
                .map(h -> new HolidayView(h.getId(), h.getHolidayDate(), h.getName(), h.getKind()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, String> holidayMap(LocalDate from, LocalDate to) {
        Map<LocalDate, String> map = new LinkedHashMap<>();
        for (Holiday holiday : holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(from, to)) {
            if (holiday.getKind() == HolidayKind.NATIONAL) {
                map.put(holiday.getHolidayDate(), holiday.getName());
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Holiday findHoliday(UUID id) {
        return holidayRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.HOLIDAY_NOT_FOUND, "Holiday not found"));
    }

    @Transactional(readOnly = true)
    public List<Holiday> holidayRows(LocalDate from, LocalDate to) {
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(from, to);
    }

    @Transactional(readOnly = true)
    AttendanceCalendar.Context context(LocalDate from, LocalDate to, Map<LocalDate, LeaveDay> leaves) {
        Map<LocalDate, String> national = new LinkedHashMap<>();
        Map<LocalDate, String> optional = new LinkedHashMap<>();
        for (Holiday holiday : holidayRepository.findByHolidayDateBetweenOrderByHolidayDate(from, to)) {
            if (holiday.getKind() == HolidayKind.NATIONAL) {
                national.put(holiday.getHolidayDate(), holiday.getName());
            } else {
                optional.put(holiday.getHolidayDate(), holiday.getName());
            }
        }
        return new AttendanceCalendar.Context(national, optional, load().weeklyOffDays(), leaves);
    }

    @Transactional
    public HolidayView addHoliday(HolidayRequest request) {
        if (holidayRepository.existsByHolidayDate(request.date())) {
            throw ApiException.conflict(ErrorCodes.HOLIDAY_EXISTS, "A holiday already exists on " + request.date());
        }
        Holiday holiday = holidayRepository.saveAndFlush(Holiday.create(request.date(), request.name().trim(), request.kind()));
        auditService.record(AuditActions.HOLIDAY_ADDED, AuditActions.ENTITY_HOLIDAY, holiday.getId().toString(),
                "Added holiday " + holiday.getName() + " on " + holiday.getHolidayDate(), null);
        return new HolidayView(holiday.getId(), holiday.getHolidayDate(), holiday.getName(), holiday.getKind());
    }

    @Transactional
    public void removeHoliday(UUID id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.HOLIDAY_NOT_FOUND, "Holiday not found"));
        if (leaveRepository.existsByHolidayIdAndStatusIn(id, LeaveService.ACTIVE)) {
            throw ApiException.conflict(ErrorCodes.HOLIDAY_IN_USE, "Staff have already claimed this optional holiday");
        }
        holidayRepository.delete(holiday);
        auditService.record(AuditActions.HOLIDAY_REMOVED, AuditActions.ENTITY_HOLIDAY, id.toString(),
                "Removed holiday " + holiday.getName() + " on " + holiday.getHolidayDate(), null);
    }

    private static SettingsView toView(AttendanceSettings s) {
        List<String> off = s.weeklyOffDays().stream().map(DayOfWeek::name).toList();
        return new SettingsView(s.getShiftStart().toString().substring(0, 5), s.getLateGraceMinutes(),
                s.getHalfDayMinutes(), s.getFullDayMinutes(), off, s.getOptionalQuota(), s.getCompOffValidDays(), s.getVersion());
    }
}
