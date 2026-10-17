package com.abchr.platform.payroll;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "salary_structure")
public class SalaryStructure extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "effective_from", nullable = false, updatable = false)
    LocalDate effectiveFrom;

    @Column(name = "gross", nullable = false)
    BigDecimal gross;

    @Column(name = "basic")
    BigDecimal basic;

    @Column(name = "hra")
    BigDecimal hra;

    @Column(name = "pf_applicable", nullable = false)
    boolean pfApplicable = true;

    @Column(name = "pt_applicable", nullable = false)
    boolean ptApplicable = true;

    @Column(name = "note", length = 200)
    String note;

    @Column(name = "ctc_annual")
    BigDecimal ctcAnnual;

    @Column(name = "variable_percent")
    BigDecimal variablePercent;

    @Column(name = "employer_pf")
    BigDecimal employerPf;

    @Column(name = "gratuity")
    BigDecimal gratuity;

    protected SalaryStructure() {
    }

    static SalaryStructure create() {
        return new SalaryStructure();
    }

    public UUID getId() {
        return id;
    }
}
