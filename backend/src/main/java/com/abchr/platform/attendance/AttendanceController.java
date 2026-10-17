package com.abchr.platform.attendance;

import com.abchr.platform.attendance.AttendanceDtos.ApplicationView;
import com.abchr.platform.attendance.AttendanceDtos.ApplyRequest;
import com.abchr.platform.attendance.AttendanceDtos.BalanceView;
import com.abchr.platform.attendance.AttendanceDtos.CheckInRequest;
import com.abchr.platform.attendance.AttendanceDtos.LeaveTypeRequest;
import com.abchr.platform.attendance.AttendanceDtos.LeaveTypeView;
import com.abchr.platform.attendance.AttendanceDtos.PreviewView;
import com.abchr.platform.attendance.AttendanceDtos.ClaimRequest;
import com.abchr.platform.attendance.AttendanceDtos.GrantCompOffRequest;
import com.abchr.platform.attendance.AttendanceDtos.LeaveOverview;
import com.abchr.platform.attendance.AttendanceDtos.LeaveRow;
import com.abchr.platform.attendance.AttendanceDtos.UseCompOffRequest;
import com.abchr.platform.attendance.AttendanceDtos.CorrectionRequest;
import com.abchr.platform.attendance.AttendanceDtos.CorrectionView;
import com.abchr.platform.attendance.AttendanceDtos.DecisionRequest;
import com.abchr.platform.attendance.AttendanceDtos.HolidayRequest;
import com.abchr.platform.attendance.AttendanceDtos.HolidayView;
import com.abchr.platform.attendance.AttendanceDtos.MarkRequest;
import com.abchr.platform.attendance.AttendanceDtos.MonthView;
import com.abchr.platform.attendance.AttendanceDtos.SettingsRequest;
import com.abchr.platform.attendance.AttendanceDtos.SettingsView;
import com.abchr.platform.attendance.AttendanceDtos.SummaryRow;
import com.abchr.platform.attendance.AttendanceDtos.TeamDay;
import com.abchr.platform.attendance.AttendanceDtos.TodayView;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private static final String SELF = "hasAnyAuthority('ATTENDANCE_SELF','ATTENDANCE_MANAGE')";
    private static final String APPROVE = "hasAnyAuthority('ATTENDANCE_APPROVE','ATTENDANCE_MANAGE')";
    private static final String MANAGE = "hasAuthority('ATTENDANCE_MANAGE')";

    private final AttendanceService service;
    private final AttendanceAdminService adminService;
    private final RegularizationService regularizationService;
    private final AttendanceSettingsService settingsService;
    private final LeaveService leaveService;
    private final LeaveTypeService leaveTypeService;
    private final LeaveApplicationService applicationService;

    public AttendanceController(
            AttendanceService service,
            AttendanceAdminService adminService,
            RegularizationService regularizationService,
            AttendanceSettingsService settingsService,
            LeaveService leaveService,
            LeaveTypeService leaveTypeService,
            LeaveApplicationService applicationService) {
        this.service = service;
        this.adminService = adminService;
        this.regularizationService = regularizationService;
        this.settingsService = settingsService;
        this.leaveService = leaveService;
        this.leaveTypeService = leaveTypeService;
        this.applicationService = applicationService;
    }

    @GetMapping("/today")
    @PreAuthorize(SELF)
    public TodayView today() {
        return service.today();
    }

    @PostMapping("/check-in")
    @PreAuthorize(SELF)
    public TodayView checkIn(@Valid @RequestBody(required = false) CheckInRequest request) {
        return service.checkIn(request);
    }

    @PostMapping("/check-out")
    @PreAuthorize(SELF)
    public TodayView checkOut() {
        return service.checkOut();
    }

    @GetMapping("/me")
    @PreAuthorize(SELF)
    public MonthView me(@RequestParam(required = false) String month) {
        return service.month(month);
    }

    @GetMapping("/users/{userId}/month")
    @PreAuthorize(MANAGE)
    public MonthView userMonth(@PathVariable UUID userId, @RequestParam(required = false) String month) {
        return service.monthFor(userId, month);
    }

    @GetMapping("/team")
    @PreAuthorize(MANAGE)
    public TeamDay team(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return adminService.team(date);
    }

    @PutMapping("/team/mark")
    @PreAuthorize(MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mark(@Valid @RequestBody MarkRequest request) {
        adminService.mark(request);
    }

    @GetMapping("/summary")
    @PreAuthorize(MANAGE)
    public List<SummaryRow> summary(@RequestParam(required = false) String month) {
        return adminService.summary(month);
    }

    @PostMapping("/requests")
    @PreAuthorize(SELF)
    @ResponseStatus(HttpStatus.CREATED)
    public CorrectionView createRequest(@Valid @RequestBody CorrectionRequest request) {
        return regularizationService.create(request);
    }

    @GetMapping("/requests/mine")
    @PreAuthorize(SELF)
    public List<CorrectionView> myRequests() {
        return regularizationService.mine();
    }

    @GetMapping("/requests")
    @PreAuthorize(MANAGE)
    public List<CorrectionView> requests(@RequestParam(required = false) RequestStatus status) {
        return regularizationService.list(status);
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize(MANAGE)
    public CorrectionView approve(@PathVariable UUID id, @Valid @RequestBody(required = false) DecisionRequest request) {
        return regularizationService.approve(id, request);
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize(MANAGE)
    public CorrectionView reject(@PathVariable UUID id, @Valid @RequestBody(required = false) DecisionRequest request) {
        return regularizationService.reject(id, request);
    }

    @GetMapping("/settings")
    @PreAuthorize(SELF)
    public SettingsView settings() {
        return settingsService.get();
    }

    @PutMapping("/settings")
    @PreAuthorize(MANAGE)
    public SettingsView updateSettings(@Valid @RequestBody SettingsRequest request) {
        return settingsService.update(request);
    }

    @GetMapping("/holidays")
    @PreAuthorize(SELF)
    public List<HolidayView> holidays(@RequestParam int year) {
        return settingsService.holidays(year);
    }

    @PostMapping("/holidays")
    @PreAuthorize(MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public HolidayView addHoliday(@Valid @RequestBody HolidayRequest request) {
        return settingsService.addHoliday(request);
    }

    @DeleteMapping("/holidays/{id}")
    @PreAuthorize(MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeHoliday(@PathVariable UUID id) {
        settingsService.removeHoliday(id);
    }

    @GetMapping("/leave-types")
    @PreAuthorize(SELF)
    public List<LeaveTypeView> leaveTypes(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return leaveTypeService.list(activeOnly && !com.abchr.platform.shared.security.Permissions
                .hasAny("ATTENDANCE_MANAGE"));
    }

    @PostMapping("/leave-types")
    @PreAuthorize(MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveTypeView createLeaveType(@Valid @RequestBody LeaveTypeRequest request) {
        return leaveTypeService.create(request);
    }

    @PutMapping("/leave-types/{id}")
    @PreAuthorize(MANAGE)
    public LeaveTypeView updateLeaveType(@PathVariable UUID id, @Valid @RequestBody LeaveTypeRequest request) {
        return leaveTypeService.update(id, request);
    }

    @DeleteMapping("/leave-types/{id}")
    @PreAuthorize(MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLeaveType(@PathVariable UUID id) {
        leaveTypeService.delete(id);
    }

    @GetMapping("/leave/balances")
    @PreAuthorize(SELF)
    public List<BalanceView> leaveBalances(@RequestParam(required = false) Integer year) {
        return applicationService.balances(year);
    }

    @GetMapping("/leave/applications/preview")
    @PreAuthorize(SELF)
    public PreviewView previewApplication(
            @RequestParam UUID leaveTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) HalfDay halfDay) {
        return applicationService.preview(leaveTypeId, fromDate, toDate, halfDay);
    }

    @GetMapping("/leave/applications/mine")
    @PreAuthorize(SELF)
    public List<ApplicationView> myApplications() {
        return applicationService.mine();
    }

    @PostMapping("/leave/applications")
    @PreAuthorize(SELF)
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationView apply(@Valid @RequestBody ApplyRequest request) {
        return applicationService.apply(request);
    }

    @DeleteMapping("/leave/applications/{id}")
    @PreAuthorize(SELF)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelApplication(@PathVariable UUID id) {
        applicationService.cancel(id);
    }

    @GetMapping("/leave/applications")
    @PreAuthorize(APPROVE)
    public List<ApplicationView> applications(@RequestParam(required = false) LeaveStatus status) {
        return applicationService.requests(status);
    }

    @PostMapping("/leave/applications/{id}/approve")
    @PreAuthorize(APPROVE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveApplication(@PathVariable UUID id,
            @Valid @RequestBody(required = false) DecisionRequest request) {
        applicationService.approve(id, request == null ? null : request.note());
    }

    @PostMapping("/leave/applications/{id}/reject")
    @PreAuthorize(APPROVE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectApplication(@PathVariable UUID id,
            @Valid @RequestBody(required = false) DecisionRequest request) {
        applicationService.reject(id, request == null ? null : request.note());
    }

    @GetMapping("/leave/overview")
    @PreAuthorize(SELF)
    public LeaveOverview leaveOverview(@RequestParam(required = false) Integer year) {
        return leaveService.overview(year);
    }

    @PostMapping("/leave/optional")
    @PreAuthorize(SELF)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void claimOptional(@Valid @RequestBody ClaimRequest request) {
        leaveService.claimOptional(request.holidayId());
    }

    @PostMapping("/leave/comp-off")
    @PreAuthorize(SELF)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void useCompOff(@Valid @RequestBody UseCompOffRequest request) {
        leaveService.useCompOff(request.date());
    }

    @DeleteMapping("/leave/{id}")
    @PreAuthorize(SELF)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelLeave(@PathVariable UUID id) {
        leaveService.cancel(id);
    }

    @GetMapping("/leave/requests")
    @PreAuthorize(APPROVE)
    public List<LeaveRow> leaveRequests(@RequestParam(required = false) LeaveStatus status) {
        return leaveService.requests(status);
    }

    @PostMapping("/leave/{id}/approve")
    @PreAuthorize(APPROVE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveLeave(@PathVariable UUID id, @Valid @RequestBody(required = false) DecisionRequest request) {
        leaveService.approve(id, request == null ? null : request.note());
    }

    @PostMapping("/leave/{id}/reject")
    @PreAuthorize(APPROVE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectLeave(@PathVariable UUID id, @Valid @RequestBody(required = false) DecisionRequest request) {
        leaveService.reject(id, request == null ? null : request.note());
    }

    @PostMapping("/leave/comp-off/grant")
    @PreAuthorize(MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grantCompOff(@Valid @RequestBody GrantCompOffRequest request) {
        leaveService.grantCompOff(request);
    }
}
