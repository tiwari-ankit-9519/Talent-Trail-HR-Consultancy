package com.abchr.platform.payroll;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.payroll.PayrollDtos.SettingsRequest;
import com.abchr.platform.payroll.PayrollDtos.SettingsView;
import com.abchr.platform.shared.persistence.VersionCheck;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollSettingsService {

    private final PayrollSettingsRepository repository;
    private final AuditService auditService;

    public PayrollSettingsService(PayrollSettingsRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public SettingsView get() {
        return toView(load());
    }

    @Transactional
    public SettingsView update(SettingsRequest request) {
        PayrollSettings settings = load();
        VersionCheck.verify(request.version(), settings.getVersion(), PayrollSettings.class, 1);
        Changes changes = Changes.create()
                .field("pfEnabled", settings.pfEnabled, request.pfEnabled())
                .field("pfRate", settings.pfRate.doubleValue(), request.pfRate().doubleValue())
                .field("pfWageCeiling", settings.pfWageCeiling.doubleValue(), request.pfWageCeiling().doubleValue())
                .field("ptAmount", settings.ptAmount.doubleValue(), request.ptAmount().doubleValue())
                .field("ptMinGross", settings.ptMinGross.doubleValue(), request.ptMinGross().doubleValue())
                .field("basicPercent", settings.basicPercent.doubleValue(), request.basicPercent().doubleValue())
                .field("hraPercent", settings.hraPercent.doubleValue(), request.hraPercent().doubleValue())
                .field("employerPfRate", settings.employerPfRate.doubleValue(), request.employerPfRate().doubleValue())
                .field("gratuityEnabled", settings.gratuityEnabled, request.gratuityEnabled())
                .field("gratuityPercent", settings.gratuityPercent.doubleValue(),
                        request.gratuityPercent().doubleValue());
        settings.pfEnabled = request.pfEnabled();
        settings.pfRate = request.pfRate();
        settings.pfWageCeiling = request.pfWageCeiling();
        settings.ptAmount = request.ptAmount();
        settings.ptMinGross = request.ptMinGross();
        settings.basicPercent = request.basicPercent();
        settings.hraPercent = request.hraPercent();
        settings.employerPfRate = request.employerPfRate();
        settings.gratuityEnabled = request.gratuityEnabled();
        settings.gratuityPercent = request.gratuityPercent();
        repository.saveAndFlush(settings);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.PAYROLL_SETTINGS_UPDATED, AuditActions.ENTITY_PAYROLL_SETTINGS, "1",
                    changes.summary("Updated payroll rules"), changes.toJson());
        }
        return toView(settings);
    }

    PayrollSettings load() {
        return repository.findById((short) 1).orElseThrow();
    }

    private static SettingsView toView(PayrollSettings s) {
        return new SettingsView(s.pfEnabled, s.pfRate, s.pfWageCeiling, s.ptAmount, s.ptMinGross, s.basicPercent,
                s.hraPercent, s.employerPfRate, s.gratuityEnabled, s.gratuityPercent, s.getVersion());
    }
}
