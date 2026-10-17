package com.abchr.platform.placement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlacementRepository extends JpaRepository<Placement, UUID>, JpaSpecificationExecutor<Placement> {

    Optional<Placement> findBySubmissionId(UUID submissionId);

    boolean existsBySubmissionId(UUID submissionId);
}
