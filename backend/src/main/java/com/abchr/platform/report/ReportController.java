package com.abchr.platform.report;

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

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
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
