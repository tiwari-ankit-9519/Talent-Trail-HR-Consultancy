package com.abchr.platform.submission;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubmissionRepository extends JpaRepository<Submission, UUID>, JpaSpecificationExecutor<Submission> {

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    List<Submission> findByCandidateIdOrderBySubmittedAtDesc(UUID candidateId);
}
