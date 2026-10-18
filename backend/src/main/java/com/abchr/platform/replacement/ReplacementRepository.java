package com.abchr.platform.replacement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReplacementRepository extends JpaRepository<Replacement, UUID>, JpaSpecificationExecutor<Replacement> {

    Optional<Replacement> findByPlacementId(UUID placementId);

    Optional<Replacement> findByReplacementPlacementId(UUID placementId);

    List<Replacement> findByPlacementIdIn(Collection<UUID> placementIds);

    List<Replacement> findByReplacementPlacementIdIn(Collection<UUID> placementIds);

    boolean existsByReplacementPlacementId(UUID placementId);
}
