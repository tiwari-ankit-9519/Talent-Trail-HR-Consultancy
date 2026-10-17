package com.abchr.platform.submission;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionFeedbackRepository extends JpaRepository<SubmissionFeedback, UUID> {

    List<SubmissionFeedback> findBySubmissionIdOrderByReceivedOnDescCreatedAtDesc(UUID submissionId);
}
