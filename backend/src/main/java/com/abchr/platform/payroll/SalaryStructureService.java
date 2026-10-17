package com.abchr.platform.payroll;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.payroll.PayrollDtos.Breakdown;
import com.abchr.platform.payroll.PayrollDtos.EmployeeRow;
import com.abchr.platform.payroll.PayrollDtos.PreviewRequest;
import com.abchr.platform.payroll.PayrollDtos.StructureRequest;
import com.abchr.platform.payroll.PayrollDtos.StructureView;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaryStructureService {

    private final SalaryStructureRepository repository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PayrollSettingsService settingsService;

    public SalaryStructureService(
            SalaryStructureRepository repository, UserRepository userRepository, AuditService auditService,
            PayrollSettingsService settingsService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public List<EmployeeRow> employees() {
        LocalDate today = LocalDate.now();
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> new EmployeeRow(user.getId(), user.getFullName(), user.getEmail(), user.isActive(),
                        effective(user.getId(), today).map(SalaryStructureService::toView).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StructureView> history(UUID userId) {
        return repository.findByUserIdOrderByEffectiveFromDesc(userId).stream()
                .map(SalaryStructureService::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Breakdown preview(PreviewRequest request) {
        return SalaryCalculator.compute(request.ctcAnnual(), request.variablePercent(), request.pfApplicable(),
                settingsService.load());
    }

    @Transactional
    public StructureView set(UUID userId, StructureRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.ATTENDANCE_USER_NOT_FOUND, "User not found"));
        Breakdown b = SalaryCalculator.compute(request.ctcAnnual(), request.variablePercent(),
                request.pfApplicable(), settingsService.load());
        if (b.gross().signum() <= 0) {
            throw ApiException.rule(ErrorCodes.SALARY_STRUCTURE_INVALID, "The fixed CTC is too small to pay a salary");
        }
        SalaryStructure structure = repository.findByUserIdAndEffectiveFrom(userId, request.effectiveFrom())
                .orElseGet(() -> {
                    SalaryStructure created = SalaryStructure.create();
                    created.userId = userId;
                    created.effectiveFrom = request.effectiveFrom();
                    return created;
                });
        structure.ctcAnnual = b.ctcAnnual();
        structure.variablePercent = request.variablePercent();
        structure.gross = b.gross();
        structure.basic = b.basic();
        structure.hra = b.hra();
        structure.employerPf = b.employerPf();
        structure.gratuity = b.gratuity();
        structure.pfApplicable = request.pfApplicable();
        structure.ptApplicable = request.ptApplicable();
        structure.note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        repository.saveAndFlush(structure);
        auditService.record(AuditActions.SALARY_STRUCTURE_SET, AuditActions.ENTITY_SALARY_STRUCTURE,
                structure.getId().toString(),
                "Set salary structure for " + user.getFullName() + " from " + request.effectiveFrom(), null);
        return toView(structure);
    }

    Optional<SalaryStructure> effective(UUID userId, LocalDate date) {
        return repository.findFirstByUserIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(userId, date);
    }

    static StructureView toView(SalaryStructure s) {
        BigDecimal allowance = s.basic == null
                ? null
                : s.gross.subtract(s.basic).subtract(s.hra == null ? BigDecimal.ZERO : s.hra);
        BigDecimal variableAnnual = s.ctcAnnual == null || s.variablePercent == null
                ? null
                : s.ctcAnnual.multiply(s.variablePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new StructureView(s.getId(), s.userId, s.effectiveFrom, s.gross, s.basic, s.hra, allowance,
                s.pfApplicable, s.ptApplicable, s.note, s.ctcAnnual, s.variablePercent, variableAnnual, s.employerPf,
                s.gratuity, s.getVersion());
    }
}
