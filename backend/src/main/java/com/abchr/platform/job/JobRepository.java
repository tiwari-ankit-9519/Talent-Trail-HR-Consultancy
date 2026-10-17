package com.abchr.platform.job;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByCodeIgnoreCase(String code);

    @Query(value = "select nextval('job_code_seq')", nativeQuery = true)
    long nextCodeNumber();
}
