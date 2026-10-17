package com.abchr.platform.payroll;

import com.abchr.platform.attendance.AttendanceDtos.Totals;
import com.abchr.platform.attendance.AttendanceService;
import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.commission.CommissionService;
import com.abchr.platform.commission.CommissionService.PayableCommission;
import com.abchr.platform.notification.NotificationService;
import com.abchr.platform.payroll.PayrollDtos.AdjustRequest;
import com.abchr.platform.payroll.PayrollDtos.CommissionLine;
import com.abchr.platform.payroll.PayrollDtos.PayslipView;
import com.abchr.platform.payroll.PayrollDtos.RunDetail;
import com.abchr.platform.payroll.PayrollDtos.RunView;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollService {

    private final PayrollRunRepository runRepository;
    private final PayslipRepository payslipRepository;
    private final SalaryStructureService structureService;
    private final PayrollSettingsService settingsService;
    private final AttendanceService attendanceService;
    private final CommissionService commissionService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;
    private final AuditService auditService;
    private final ZoneId zone;

    public PayrollService(
            PayrollRunRepository runRepository,
            PayslipRepository payslipRepository,
            SalaryStructureService structureService,
            PayrollSettingsService settingsService,
            AttendanceService attendanceService,
            CommissionService commissionService,
            NotificationService notificationService,
            UserRepository userRepository,
            JdbcTemplate jdbc,
            AuditService auditService,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.runRepository = runRepository;
        this.payslipRepository = payslipRepository;
        this.structureService = structureService;
        this.settingsService = settingsService;
        this.attendanceService = attendanceService;
        this.commissionService = commissionService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
        this.auditService = auditService;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public List<RunView> runs() {
        return runRepository.findAllByOrderByMonthDesc().stream()
                .map(run -> runView(run, payslipRepository.findByRunIdOrderByEmployeeName(run.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RunDetail detail(String month) {
        PayrollRun run = runRepository.findByMonth(month)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.PAYROLL_NOT_FOUND, "No payroll for " + month));
        return detailOf(run);
    }

    @Transactional
    public RunDetail generate(String month) {
        YearMonth ym = parse(month);
        if (ym.isAfter(YearMonth.now(zone))) {
            throw ApiException.rule(ErrorCodes.PAYROLL_MONTH_INVALID, "Payroll cannot be run for a future month");
        }
        PayrollRun run = runRepository.findByMonth(ym.toString()).orElseGet(() -> PayrollRun.create(ym.toString()));
        if (run.status == RunStatus.FINALIZED) {
            throw ApiException.conflict(ErrorCodes.PAYROLL_LOCKED, "This month is finalized");
        }
        runRepository.saveAndFlush(run);
        int count = recompute(run, ym);
        if (count == 0) {
            throw ApiException.rule(ErrorCodes.PAYROLL_NO_EMPLOYEES, "No employee has a salary structure for this month");
        }
        auditService.record(AuditActions.PAYROLL_GENERATED, AuditActions.ENTITY_PAYROLL_RUN, run.getId().toString(),
                "Calculated payroll for " + ym + " (" + count + " employees)", null);
        return detailOf(run);
    }

    @Transactional
    public RunDetail finalizeRun(String month) {
        YearMonth ym = parse(month);
        PayrollRun run = runRepository.findByMonth(ym.toString())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.PAYROLL_NOT_FOUND, "No payroll for " + month));
        if (run.status == RunStatus.FINALIZED) {
            throw ApiException.conflict(ErrorCodes.PAYROLL_LOCKED, "This month is already finalized");
        }
        if (!ym.isBefore(YearMonth.now(zone))) {
            throw ApiException.rule(ErrorCodes.PAYROLL_MONTH_OPEN, "The month must be over before it is finalized");
        }
        recompute(run, ym);
        List<Payslip> slips = payslipRepository.findByRunIdOrderByEmployeeName(run.getId());
        for (Payslip slip : slips) {
            for (CommissionLine line : commissionLines(slip.getId())) {
                commissionService.payWithSalary(line.commissionId(), ym.atEndOfMonth(), "Salary " + ym);
            }
        }
        run.status = RunStatus.FINALIZED;
        run.finalizedBy = Actor.current().email();
        run.finalizedAt = Instant.now();
        runRepository.saveAndFlush(run);
        auditService.record(AuditActions.PAYROLL_FINALIZED, AuditActions.ENTITY_PAYROLL_RUN, run.getId().toString(),
                "Finalized payroll for " + ym, null);
        notificationService.notifyUsers(slips.stream().map(s -> s.userId).toList(), "payroll", "Payslip ready",
                "Your payslip for " + ym + " is available", "/payslips");
        return detailOf(run);
    }

    @Transactional
    public PayslipView adjust(UUID id, AdjustRequest request) {
        Payslip slip = payslipRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.PAYSLIP_NOT_FOUND, "Payslip not found"));
        PayrollRun run = runRepository.findById(slip.runId).orElseThrow();
        if (run.status != RunStatus.DRAFT) {
            throw ApiException.conflict(ErrorCodes.PAYROLL_LOCKED, "This month is finalized");
        }
        VersionCheck.verify(request.version(), slip.getVersion(), Payslip.class, id);
        Changes changes = Changes.create()
                .field("otherEarnings", slip.otherEarnings.doubleValue(), request.otherEarnings().doubleValue())
                .field("tds", slip.tds.doubleValue(), request.tds().doubleValue())
                .field("otherDeductions", slip.otherDeductions.doubleValue(), request.otherDeductions().doubleValue());
        slip.otherEarnings = money(request.otherEarnings());
        slip.earningsNote = clean(request.earningsNote());
        slip.tds = money(request.tds());
        slip.otherDeductions = money(request.otherDeductions());
        slip.deductionsNote = clean(request.deductionsNote());
        slip.net = net(slip);
        payslipRepository.saveAndFlush(slip);
        auditService.record(AuditActions.PAYSLIP_ADJUSTED, AuditActions.ENTITY_PAYSLIP, id.toString(),
                changes.summary("Adjusted payslip of " + slip.employeeName + " for " + run.month),
                changes.toJson());
        return view(slip, run);
    }

    @Transactional(readOnly = true)
    public List<PayslipView> mine() {
        UUID userId = Actor.current().id();
        Map<UUID, PayrollRun> runs = new HashMap<>();
        return payslipRepository.findByUserId(userId).stream()
                .map(slip -> Map.entry(slip, runs.computeIfAbsent(slip.runId, k -> runRepository.findById(k).orElseThrow())))
                .filter(entry -> entry.getValue().status == RunStatus.FINALIZED)
                .sorted((a, b) -> b.getValue().month.compareTo(a.getValue().month))
                .map(entry -> view(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PayslipView payslip(UUID id) {
        Payslip slip = payslipRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.PAYSLIP_NOT_FOUND, "Payslip not found"));
        PayrollRun run = runRepository.findById(slip.runId).orElseThrow();
        boolean manager = Permissions.hasAny("PAYROLL_MANAGE");
        boolean owner = slip.userId.equals(Actor.current().id()) && run.status == RunStatus.FINALIZED;
        if (!manager && !owner) {
            throw ApiException.notFound(ErrorCodes.PAYSLIP_NOT_FOUND, "Payslip not found");
        }
        return view(slip, run);
    }

    private int recompute(PayrollRun run, YearMonth ym) {
        PayrollSettings settings = settingsService.load();
        Map<UUID, Payslip> existing = new HashMap<>();
        for (Payslip slip : payslipRepository.findByRunIdOrderByEmployeeName(run.getId())) {
            existing.put(slip.userId, slip);
        }
        Set<UUID> seen = new HashSet<>();
        for (AppUser user : userRepository.findAll()) {
            if (!user.isActive()) {
                continue;
            }
            SalaryStructure structure = structureService.effective(user.getId(), ym.atEndOfMonth()).orElse(null);
            if (structure == null) {
                continue;
            }
            Payslip slip = existing.getOrDefault(user.getId(), Payslip.create(run.getId(), user.getId()));
            compute(slip, user, structure, settings, ym);
            payslipRepository.saveAndFlush(slip);
            replaceCommissions(slip, user.getId());
            seen.add(user.getId());
        }
        existing.forEach((userId, slip) -> {
            if (!seen.contains(userId)) {
                payslipRepository.delete(slip);
            }
        });
        payslipRepository.flush();
        return seen.size();
    }

    private void compute(Payslip slip, AppUser user, SalaryStructure st, PayrollSettings settings, YearMonth ym) {
        java.time.LocalDate from = st.effectiveFrom.isAfter(ym.atDay(1)) ? st.effectiveFrom : ym.atDay(1);
        Totals totals = attendanceService.totalsFrom(user.getId(), ym.toString(), from);
        int days = ym.lengthOfMonth();
        BigDecimal payable = BigDecimal.valueOf(totals.payableDays()).setScale(1, RoundingMode.HALF_UP);
        slip.employeeName = user.getFullName();
        slip.employeeEmail = user.getEmail();
        slip.calendarDays = days;
        slip.payableDays = payable;
        slip.leaveDays = BigDecimal.valueOf(totals.leaves()).setScale(1, RoundingMode.HALF_UP);
        slip.lossOfPayDays = BigDecimal.valueOf(totals.lossOfPay()).setScale(1, RoundingMode.HALF_UP);
        slip.gross = st.gross;
        slip.earnedGross = prorate(st.gross, payable, days);
        if (st.basic == null) {
            slip.basic = null;
            slip.hra = null;
            slip.allowance = null;
        } else {
            slip.basic = prorate(st.basic, payable, days);
            slip.hra = st.hra == null ? null : prorate(st.hra, payable, days);
            slip.allowance = slip.earnedGross.subtract(slip.basic)
                    .subtract(slip.hra == null ? BigDecimal.ZERO : slip.hra);
        }
        BigDecimal pfWage = slip.basic != null ? slip.basic : slip.earnedGross;
        if (settings.pfWageCeiling.signum() > 0 && pfWage.compareTo(settings.pfWageCeiling) > 0) {
            pfWage = settings.pfWageCeiling;
        }
        slip.pf = settings.pfEnabled && st.pfApplicable
                ? pfWage.multiply(settings.pfRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);
        slip.pt = st.ptApplicable && slip.earnedGross.signum() > 0 && st.gross.compareTo(settings.ptMinGross) >= 0
                ? settings.ptAmount
                : BigDecimal.ZERO.setScale(2);
        BigDecimal commission = BigDecimal.ZERO.setScale(2);
        for (PayableCommission c : commissionService.payableFor(user.getId())) {
            commission = commission.add(c.amount());
        }
        slip.commission = commission;
        slip.net = net(slip);
    }

    private void replaceCommissions(Payslip slip, UUID userId) {
        jdbc.update("delete from payslip_commission where payslip_id = ?", slip.getId());
        for (PayableCommission c : commissionService.payableFor(userId)) {
            jdbc.update("insert into payslip_commission (payslip_id, commission_id, amount, label) values (?, ?, ?, ?)",
                    slip.getId(), c.id(), c.amount(), c.label());
        }
    }

    private List<CommissionLine> commissionLines(UUID payslipId) {
        return jdbc.query("select commission_id, amount, label from payslip_commission where payslip_id = ? order by label",
                (rs, row) -> new CommissionLine((UUID) rs.getObject(1), rs.getBigDecimal(2), rs.getString(3)), payslipId);
    }

    private RunDetail detailOf(PayrollRun run) {
        List<Payslip> slips = payslipRepository.findByRunIdOrderByEmployeeName(run.getId());
        return new RunDetail(runView(run, slips), slips.stream().map(s -> view(s, run)).toList());
    }

    private RunView runView(PayrollRun run, List<Payslip> slips) {
        BigDecimal earnings = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (Payslip s : slips) {
            earnings = earnings.add(totalEarnings(s));
            deductions = deductions.add(totalDeductions(s));
            net = net.add(s.net);
        }
        return new RunView(run.getId(), run.month, run.status, run.finalizedBy, run.finalizedAt, slips.size(),
                earnings, deductions, net, run.getVersion());
    }

    private PayslipView view(Payslip s, PayrollRun run) {
        return new PayslipView(s.getId(), run.getId(), run.month, run.status, s.userId, s.employeeName,
                s.employeeEmail, s.calendarDays, s.payableDays, s.leaveDays, s.lossOfPayDays, s.gross, s.earnedGross,
                s.basic, s.hra, s.allowance, s.commission, s.otherEarnings, s.earningsNote, s.pf, s.pt, s.tds,
                s.otherDeductions, s.deductionsNote, totalEarnings(s), totalDeductions(s), s.net,
                commissionLines(s.getId()), s.getVersion());
    }

    private static BigDecimal totalEarnings(Payslip s) {
        return s.earnedGross.add(s.commission).add(s.otherEarnings);
    }

    private static BigDecimal totalDeductions(Payslip s) {
        return s.pf.add(s.pt).add(s.tds).add(s.otherDeductions);
    }

    private static BigDecimal net(Payslip s) {
        return totalEarnings(s).subtract(totalDeductions(s));
    }

    private static BigDecimal prorate(BigDecimal amount, BigDecimal payable, int days) {
        return amount.multiply(payable).divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw ApiException.rule(ErrorCodes.PAYROLL_MONTH_INVALID, "Month must look like 2026-10");
        }
    }
}
