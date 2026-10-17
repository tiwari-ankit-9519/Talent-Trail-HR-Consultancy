package com.abchr.platform.report;

import com.abchr.platform.report.BusinessReportDtos.CommissionReport;
import com.abchr.platform.report.BusinessReportDtos.LeaveReport;
import com.abchr.platform.report.BusinessReportDtos.PayrollRegister;
import com.abchr.platform.report.BusinessReportDtos.ReceivablesReport;
import com.abchr.platform.report.BusinessReportDtos.TaxSummary;
import com.abchr.platform.report.ReportDtos.FunnelReport;
import com.abchr.platform.report.ReportDtos.JoiningsReport;
import com.abchr.platform.report.ReportDtos.RecruiterRow;
import com.abchr.platform.report.ReportDtos.ReplacementRow;
import com.abchr.platform.report.ReportDtos.RevenueReport;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final String VIEW = "hasAuthority('REPORTS_VIEW')";
    private static final String REVENUE = "hasAuthority('REPORTS_VIEW') and hasAnyAuthority('INVOICES_VIEW','INVOICES_MANAGE')";

    private static final String PAYROLL = "hasAuthority('PAYROLL_MANAGE')";
    private static final String ATTENDANCE = "hasAuthority('ATTENDANCE_MANAGE')";
    private static final String INVOICES = "hasAnyAuthority('INVOICES_VIEW','INVOICES_MANAGE')";
    private static final String COMMISSIONS = "hasAnyAuthority('COMMISSIONS_VIEW','COMMISSIONS_MANAGE')";

    private final ReportService service;
    private final BusinessReportService businessService;

    public ReportController(ReportService service, BusinessReportService businessService) {
        this.service = service;
        this.businessService = businessService;
    }

    @GetMapping("/payroll-register")
    @PreAuthorize(PAYROLL)
    public PayrollRegister payrollRegister(@RequestParam(required = false) String month) {
        return businessService.payrollRegister(month);
    }

    @GetMapping("/leave")
    @PreAuthorize(ATTENDANCE)
    public LeaveReport leave(@RequestParam(required = false) Integer year) {
        return businessService.leave(year);
    }

    @GetMapping("/receivables")
    @PreAuthorize(INVOICES)
    public ReceivablesReport receivables(@RequestParam(required = false) UUID clientId) {
        return businessService.receivables(clientId);
    }

    @GetMapping("/tax-summary")
    @PreAuthorize(INVOICES)
    public TaxSummary taxSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clientId) {
        return businessService.taxSummary(from, to, clientId);
    }

    @GetMapping("/commissions")
    @PreAuthorize(COMMISSIONS)
    public CommissionReport commissions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {
        return businessService.commissions(from, to, status);
    }

    @GetMapping("/joinings")
    @PreAuthorize(VIEW)
    public JoiningsReport joinings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clientId) {
        return service.joinings(from, to, clientId);
    }

    @GetMapping("/revenue")
    @PreAuthorize(REVENUE)
    public RevenueReport revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clientId) {
        return service.revenue(from, to, clientId);
    }

    @GetMapping("/recruiters")
    @PreAuthorize(VIEW)
    public List<RecruiterRow> recruiters(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clientId) {
        return service.recruiters(from, to, clientId);
    }

    @GetMapping("/funnel")
    @PreAuthorize(VIEW)
    public FunnelReport funnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clientId) {
        return service.funnel(from, to, clientId);
    }

    @GetMapping("/replacements")
    @PreAuthorize(VIEW)
    public List<ReplacementRow> replacements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clientId) {
        return service.replacements(from, to, clientId);
    }
}
