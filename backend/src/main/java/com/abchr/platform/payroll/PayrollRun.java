package com.abchr.platform.payroll;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payroll_run")
public class PayrollRun extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "month", nullable = false, updatable = false, length = 7)
    String month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    RunStatus status = RunStatus.DRAFT;

    @Column(name = "finalized_by", length = 255)
    String finalizedBy;

    @Column(name = "finalized_at")
    Instant finalizedAt;

    protected PayrollRun() {
    }

    static PayrollRun create(String month) {
        PayrollRun run = new PayrollRun();
        run.month = month;
        return run;
    }

    public UUID getId() {
        return id;
    }
}
