package com.abchr.platform.payroll;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayslipRepository extends JpaRepository<Payslip, UUID> {

    List<Payslip> findByRunIdOrderByEmployeeName(UUID runId);

    Optional<Payslip> findByRunIdAndUserId(UUID runId, UUID userId);

    List<Payslip> findByUserId(UUID userId);

    long countByRunId(UUID runId);
}
