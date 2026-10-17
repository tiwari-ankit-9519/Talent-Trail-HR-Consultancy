package com.abchr.platform.payroll;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "payroll_settings")
public class PayrollSettings extends AuditableEntity {

    @Id
    private Short id = 1;

    @Column(name = "pf_enabled", nullable = false)
    boolean pfEnabled = true;

    @Column(name = "pf_rate", nullable = false)
    BigDecimal pfRate = BigDecimal.valueOf(12);

    @Column(name = "pf_wage_ceiling", nullable = false)
    BigDecimal pfWageCeiling = BigDecimal.valueOf(15000);

    @Column(name = "pt_amount", nullable = false)
    BigDecimal ptAmount = BigDecimal.valueOf(200);

    @Column(name = "pt_min_gross", nullable = false)
    BigDecimal ptMinGross = BigDecimal.ZERO;

    @Column(name = "basic_percent", nullable = false)
    BigDecimal basicPercent = BigDecimal.valueOf(50);

    @Column(name = "hra_percent", nullable = false)
    BigDecimal hraPercent = BigDecimal.valueOf(50);

    @Column(name = "employer_pf_rate", nullable = false)
    BigDecimal employerPfRate = BigDecimal.valueOf(12);

    @Column(name = "gratuity_enabled", nullable = false)
    boolean gratuityEnabled = true;

    @Column(name = "gratuity_percent", nullable = false)
    BigDecimal gratuityPercent = new BigDecimal("4.81");

    protected PayrollSettings() {
    }
}
