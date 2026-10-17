package com.abchr.platform.payroll;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, UUID> {

    List<SalaryStructure> findByUserIdOrderByEffectiveFromDesc(UUID userId);

    Optional<SalaryStructure> findByUserIdAndEffectiveFrom(UUID userId, LocalDate effectiveFrom);

    Optional<SalaryStructure> findFirstByUserIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            UUID userId, LocalDate date);

    List<SalaryStructure> findAllByOrderByEffectiveFromDesc();
}
