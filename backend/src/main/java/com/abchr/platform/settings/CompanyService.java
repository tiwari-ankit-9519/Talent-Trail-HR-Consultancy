package com.abchr.platform.settings;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.settings.CompanyDtos.CompanyRequest;
import com.abchr.platform.settings.CompanyDtos.CompanyResponse;
import com.abchr.platform.shared.persistence.VersionCheck;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyProfileRepository repository;
    private final AuditService auditService;

    public CompanyService(CompanyProfileRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public CompanyResponse get() {
        return toResponse(load());
    }

    @Transactional(readOnly = true)
    public CompanyProfile load() {
        return repository.findById((short) 1).orElseThrow();
    }

    @Transactional
    public CompanyResponse update(CompanyRequest request) {
        CompanyProfile profile = load();
        VersionCheck.verify(request.version(), profile.getVersion(), CompanyProfile.class, 1);
        Changes changes = Changes.create()
                .field("legalName", profile.getLegalName(), request.legalName().trim())
                .field("state", profile.getState(), blank(request.state()))
                .field("gstin", profile.getGstin(), upper(request.gstin()))
                .field("pan", profile.getPan(), upper(request.pan()))
                .field("sacCode", profile.getSacCode(), request.sacCode().trim())
                .field("invoicePrefix", profile.getInvoicePrefix(), request.invoicePrefix().trim().toUpperCase(Locale.ROOT))
                .field("bankAccountNumber", mask(profile.getBankAccountNumber()), mask(blank(request.bankAccountNumber())))
                .field("bankIfsc", profile.getBankIfsc(), upper(request.bankIfsc()));
        profile.setLegalName(request.legalName().trim());
        profile.setAddressLine1(blank(request.addressLine1()));
        profile.setAddressLine2(blank(request.addressLine2()));
        profile.setCity(blank(request.city()));
        profile.setState(blank(request.state()));
        profile.setPostalCode(blank(request.postalCode()));
        profile.setGstin(upper(request.gstin()));
        profile.setPan(upper(request.pan()));
        profile.setEmail(blank(request.email()));
        profile.setPhone(blank(request.phone()));
        profile.setSacCode(request.sacCode().trim());
        profile.setInvoicePrefix(request.invoicePrefix().trim().toUpperCase(Locale.ROOT));
        profile.setBankAccountName(blank(request.bankAccountName()));
        profile.setBankName(blank(request.bankName()));
        profile.setBankAccountNumber(blank(request.bankAccountNumber()));
        profile.setBankIfsc(upper(request.bankIfsc()));
        profile.setBankBranch(blank(request.bankBranch()));
        profile.setInvoiceFooter(blank(request.invoiceFooter()));
        repository.saveAndFlush(profile);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.COMPANY_UPDATED, AuditActions.ENTITY_COMPANY, "1",
                    changes.summary("Updated company profile"), changes.toJson());
        }
        return toResponse(profile);
    }

    private static CompanyResponse toResponse(CompanyProfile p) {
        return new CompanyResponse(p.getLegalName(), p.getAddressLine1(), p.getAddressLine2(), p.getCity(),
                p.getState(), p.getPostalCode(), p.getGstin(), p.getPan(), p.getEmail(), p.getPhone(), p.getSacCode(),
                p.getInvoicePrefix(), p.getBankAccountName(), p.getBankName(), p.getBankAccountNumber(),
                p.getBankIfsc(), p.getBankBranch(), p.getInvoiceFooter(), p.getVersion());
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upper(String value) {
        String trimmed = blank(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String mask(String number) {
        if (number == null || number.length() <= 4) {
            return number == null ? null : "****";
        }
        return "****" + number.substring(number.length() - 4);
    }
}
