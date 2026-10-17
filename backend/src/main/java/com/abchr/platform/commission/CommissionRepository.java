package com.abchr.platform.commission;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommissionRepository extends JpaRepository<Commission, UUID>, JpaSpecificationExecutor<Commission> {

    Optional<Commission> findByPlacementId(UUID placementId);
}
