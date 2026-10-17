package com.abchr.platform.replacement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReplacementRepository extends JpaRepository<Replacement, UUID>, JpaSpecificationExecutor<Replacement> {

    Optional<Replacement> findByPlacementId(UUID placementId);

    Optional<Replacement> findByReplacementPlacementId(UUID placementId);

    boolean existsByReplacementPlacementId(UUID placementId);
}
