package com.abchr.platform.candidate;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateConsentEventRepository extends JpaRepository<CandidateConsentEvent, UUID> {

    List<CandidateConsentEvent> findByCandidateIdOrderByRecordedAtDesc(UUID candidateId);
}
