package com.abchr.platform.submission;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "submission_feedback")
public class SubmissionFeedback extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Column(name = "interview_id", updatable = false)
    private UUID interviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private FeedbackDecision decision;

    @Column(name = "comments", columnDefinition = "text")
    private String comments;

    @Column(name = "received_on", nullable = false)
    private LocalDate receivedOn;

    protected SubmissionFeedback() {
    }

    public SubmissionFeedback(
            UUID submissionId, UUID interviewId, FeedbackDecision decision, String comments, LocalDate receivedOn) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.interviewId = interviewId;
        this.decision = decision;
        this.comments = comments;
        this.receivedOn = receivedOn;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public UUID getInterviewId() {
        return interviewId;
    }

    public FeedbackDecision getDecision() {
        return decision;
    }

    public String getComments() {
        return comments;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }
}
