package com.abchr.platform.submission;

import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submission")
public class Submission extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @Column(name = "recipients", nullable = false, columnDefinition = "text")
    private String recipients;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    private EmailStatus emailStatus = EmailStatus.PENDING;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "email_error", length = 500)
    private String emailError;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected Submission() {
    }

    public Submission(UUID jobId, UUID candidateId, UUID clientId, String recipients, String note) {
        this.id = UUID.randomUUID();
        this.jobId = jobId;
        this.candidateId = candidateId;
        this.clientId = clientId;
        this.recipients = recipients;
        this.note = note;
        this.submittedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public String getRecipients() {
        return recipients;
    }

    public String getNote() {
        return note;
    }

    public EmailStatus getEmailStatus() {
        return emailStatus;
    }

    public Instant getEmailSentAt() {
        return emailSentAt;
    }

    public String getEmailError() {
        return emailError;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void recordEmail(EmailStatus status, String error) {
        this.emailStatus = status;
        this.emailError = error;
        this.emailSentAt = status == EmailStatus.SENT ? Instant.now() : this.emailSentAt;
    }

    public boolean isClosed() {
        return status == SubmissionStatus.REJECTED
                || status == SubmissionStatus.WITHDRAWN
                || status == SubmissionStatus.SELECTED;
    }
}
