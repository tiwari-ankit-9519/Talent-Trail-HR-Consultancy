package com.abchr.platform.payroll;

import com.abchr.platform.payroll.PayrollDtos.AdjustRequest;
import com.abchr.platform.payroll.PayrollDtos.Breakdown;
import com.abchr.platform.payroll.PayrollDtos.EmployeeRow;
import com.abchr.platform.payroll.PayrollDtos.PreviewRequest;
import com.abchr.platform.payroll.PayrollDtos.PayslipView;
import com.abchr.platform.payroll.PayrollDtos.RunDetail;
import com.abchr.platform.payroll.PayrollDtos.RunRequest;
import com.abchr.platform.payroll.PayrollDtos.RunView;
import com.abchr.platform.payroll.PayrollDtos.SettingsRequest;
import com.abchr.platform.payroll.PayrollDtos.SettingsView;
import com.abchr.platform.payroll.PayrollDtos.StructureRequest;
import com.abchr.platform.payroll.PayrollDtos.StructureView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payroll")
public class PayrollController {

    private static final String MANAGE = "hasAuthority('PAYROLL_MANAGE')";
    private static final String SELF = "hasAnyAuthority('PAYROLL_SELF','PAYROLL_MANAGE')";

    private final PayrollService payrollService;
    private final SalaryStructureService structureService;
    private final PayrollSettingsService settingsService;

    public PayrollController(
            PayrollService payrollService, SalaryStructureService structureService,
            PayrollSettingsService settingsService) {
        this.payrollService = payrollService;
        this.structureService = structureService;
        this.settingsService = settingsService;
    }

    @GetMapping("/settings")
    @PreAuthorize(MANAGE)
    public SettingsView settings() {
        return settingsService.get();
    }

    @PutMapping("/settings")
    @PreAuthorize(MANAGE)
    public SettingsView updateSettings(@Valid @RequestBody SettingsRequest request) {
        return settingsService.update(request);
    }

    @GetMapping("/employees")
    @PreAuthorize(MANAGE)
    public List<EmployeeRow> employees() {
        return structureService.employees();
    }

    @GetMapping("/employees/{userId}/structures")
    @PreAuthorize(MANAGE)
    public List<StructureView> history(@PathVariable UUID userId) {
        return structureService.history(userId);
    }

    @PutMapping("/employees/{userId}/structure")
    @PreAuthorize(MANAGE)
    public StructureView setStructure(@PathVariable UUID userId, @Valid @RequestBody StructureRequest request) {
        return structureService.set(userId, request);
    }

    @PostMapping("/structures/preview")
    @PreAuthorize(MANAGE)
    public Breakdown preview(@Valid @RequestBody PreviewRequest request) {
        return structureService.preview(request);
    }

    @GetMapping("/runs")
    @PreAuthorize(MANAGE)
    public List<RunView> runs() {
        return payrollService.runs();
    }

    @PostMapping("/runs")
    @PreAuthorize(MANAGE)
    public RunDetail generate(@Valid @RequestBody RunRequest request) {
        return payrollService.generate(request.month());
    }

    @GetMapping("/runs/{month}")
    @PreAuthorize(MANAGE)
    public RunDetail detail(@PathVariable String month) {
        return payrollService.detail(month);
    }

    @PostMapping("/runs/{month}/finalize")
    @PreAuthorize(MANAGE)
    public RunDetail finalizeRun(@PathVariable String month) {
        return payrollService.finalizeRun(month);
    }

    @PutMapping("/payslips/{id}")
    @PreAuthorize(MANAGE)
    public PayslipView adjust(@PathVariable UUID id, @Valid @RequestBody AdjustRequest request) {
        return payrollService.adjust(id, request);
    }

    @GetMapping("/payslips/mine")
    @PreAuthorize(SELF)
    public List<PayslipView> mine() {
        return payrollService.mine();
    }

    @GetMapping("/payslips/{id}")
    @PreAuthorize(SELF)
    public PayslipView payslip(@PathVariable UUID id) {
        return payrollService.payslip(id);
    }
}
