package com.abchr.platform.submission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewRepository extends JpaRepository<Interview, UUID>, JpaSpecificationExecutor<Interview> {

    List<Interview> findBySubmissionIdOrderByRoundNumberAsc(UUID submissionId);

    Optional<Interview> findByIdAndSubmissionId(UUID id, UUID submissionId);

    @Query("select coalesce(max(i.roundNumber), 0) from Interview i where i.submissionId = :submissionId")
    int maxRound(@Param("submissionId") UUID submissionId);
}
