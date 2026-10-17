package com.abchr.platform.payroll;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    Optional<PayrollRun> findByMonth(String month);

    List<PayrollRun> findAllByOrderByMonthDesc();
}
