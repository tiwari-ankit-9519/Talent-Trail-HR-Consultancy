package com.abchr.platform.candidate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    Optional<JobApplication> findByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    List<JobApplication> findByCandidateIdOrderByAppliedAtDesc(UUID candidateId);

    List<JobApplication> findByJobIdOrderByAppliedAtDesc(UUID jobId);

    long countByJobId(UUID jobId);

    @Query("select a.jobId as jobId, count(a) as total from JobApplication a where a.jobId in :jobIds group by a.jobId")
    List<JobCount> countByJobIds(@Param("jobIds") List<UUID> jobIds);

    interface JobCount {
        UUID getJobId();

        long getTotal();
    }
}
