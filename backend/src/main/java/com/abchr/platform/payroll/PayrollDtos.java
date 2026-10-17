package com.abchr.platform.payroll;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PayrollDtos {

    private PayrollDtos() {
    }

    public record SettingsView(
            boolean pfEnabled, BigDecimal pfRate, BigDecimal pfWageCeiling, BigDecimal ptAmount,
            BigDecimal ptMinGross, BigDecimal basicPercent, BigDecimal hraPercent, BigDecimal employerPfRate,
            boolean gratuityEnabled, BigDecimal gratuityPercent, Long version) {
    }

    public record SettingsRequest(
            boolean pfEnabled,
            @NotNull @DecimalMin("0") BigDecimal pfRate,
            @NotNull @DecimalMin("0") BigDecimal pfWageCeiling,
            @NotNull @DecimalMin("0") BigDecimal ptAmount,
            @NotNull @DecimalMin("0") BigDecimal ptMinGross,
            @NotNull @DecimalMin("1") @DecimalMax("100") BigDecimal basicPercent,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal hraPercent,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal employerPfRate,
            boolean gratuityEnabled,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal gratuityPercent,
            Long version) {
    }

    public record Breakdown(
            BigDecimal ctcAnnual, BigDecimal variablePercent, BigDecimal variableAnnual, BigDecimal fixedAnnual,
            BigDecimal gross, BigDecimal basic, BigDecimal hra, BigDecimal allowance, BigDecimal employerPf,
            BigDecimal gratuity) {
    }

    public record PreviewRequest(
            @NotNull @DecimalMin("0") BigDecimal ctcAnnual,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal variablePercent,
            boolean pfApplicable) {
    }

    public record StructureView(
            UUID id, UUID userId, LocalDate effectiveFrom, BigDecimal gross, BigDecimal basic, BigDecimal hra,
            BigDecimal allowance, boolean pfApplicable, boolean ptApplicable, String note, BigDecimal ctcAnnual,
            BigDecimal variablePercent, BigDecimal variableAnnual, BigDecimal employerPf, BigDecimal gratuity,
            Long version) {
    }

    public record StructureRequest(
            @NotNull LocalDate effectiveFrom,
            @NotNull @DecimalMin("0") BigDecimal ctcAnnual,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal variablePercent,
            boolean pfApplicable,
            boolean ptApplicable,
            @Size(max = 200) String note) {
    }

    public record EmployeeRow(UUID userId, String name, String email, boolean active, StructureView current) {
    }

    public record RunView(
            UUID id, String month, RunStatus status, String finalizedBy, Instant finalizedAt, int employees,
            BigDecimal totalGross, BigDecimal totalDeductions, BigDecimal totalNet, Long version) {
    }

    public record RunRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String month) {
    }

    public record CommissionLine(UUID commissionId, BigDecimal amount, String label) {
    }

    public record PayslipView(
            UUID id, UUID runId, String month, RunStatus status, UUID userId, String employeeName,
            String employeeEmail, int calendarDays, BigDecimal payableDays, BigDecimal leaveDays,
            BigDecimal lossOfPayDays, BigDecimal gross, BigDecimal earnedGross, BigDecimal basic, BigDecimal hra,
            BigDecimal allowance, BigDecimal commission, BigDecimal otherEarnings, String earningsNote,
            BigDecimal pf, BigDecimal pt, BigDecimal tds, BigDecimal otherDeductions, String deductionsNote,
            BigDecimal totalEarnings, BigDecimal totalDeductions, BigDecimal net, List<CommissionLine> commissions,
            Long version) {
    }

    public record RunDetail(RunView run, List<PayslipView> payslips) {
    }

    public record AdjustRequest(
            @NotNull @DecimalMin("0") BigDecimal otherEarnings,
            @Size(max = 200) String earningsNote,
            @NotNull @DecimalMin("0") BigDecimal tds,
            @NotNull @DecimalMin("0") BigDecimal otherDeductions,
            @Size(max = 200) String deductionsNote,
            Long version) {
    }
}
