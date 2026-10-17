package com.abchr.platform.profile;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, UUID> {

    boolean existsByEmployeeCodeIgnoreCaseAndUserIdNot(String employeeCode, UUID userId);

    Optional<EmployeeProfile> findByEmployeeCodeIgnoreCase(String employeeCode);
}
