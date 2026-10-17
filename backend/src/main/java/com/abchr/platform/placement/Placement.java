package com.abchr.platform.placement;

import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "placement")
public class Placement extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlacementStatus status = PlacementStatus.OFFERED;

    @Column(name = "offered_ctc", nullable = false, precision = 12, scale = 2)
    private BigDecimal offeredCtc;

    @Column(name = "offer_date", nullable = false)
    private LocalDate offerDate;

    @Column(name = "expected_joining_date", nullable = false)
    private LocalDate expectedJoiningDate;

    @Column(name = "accepted_on")
    private LocalDate acceptedOn;

    @Column(name = "close_reason", length = 500)
    private String closeReason;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "joining_email_path", length = 500)
    private String joiningEmailPath;

    @Column(name = "joining_email_file_name", length = 255)
    private String joiningEmailFileName;

    @Column(name = "joining_email_content_type", length = 100)
    private String joiningEmailContentType;

    @Column(name = "joining_email_size")
    private Long joiningEmailSize;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_email_status", nullable = false, length = 20)
    private EmailStatus offerEmailStatus = EmailStatus.SKIPPED;

    @Column(name = "offer_email_sent_at")
    private Instant offerEmailSentAt;

    @Column(name = "offer_email_error", length = 500)
    private String offerEmailError;

    protected Placement() {
    }

    public Placement(UUID submissionId, UUID jobId, UUID candidateId, UUID clientId) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.jobId = jobId;
        this.candidateId = candidateId;
        this.clientId = clientId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
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

    public PlacementStatus getStatus() {
        return status;
    }

    public void setStatus(PlacementStatus status) {
        this.status = status;
    }

    public BigDecimal getOfferedCtc() {
        return offeredCtc;
    }

    public void setOfferedCtc(BigDecimal offeredCtc) {
        this.offeredCtc = offeredCtc;
    }

    public LocalDate getOfferDate() {
        return offerDate;
    }

    public void setOfferDate(LocalDate offerDate) {
        this.offerDate = offerDate;
    }

    public LocalDate getExpectedJoiningDate() {
        return expectedJoiningDate;
    }

    public void setExpectedJoiningDate(LocalDate expectedJoiningDate) {
        this.expectedJoiningDate = expectedJoiningDate;
    }

    public LocalDate getAcceptedOn() {
        return acceptedOn;
    }

    public void setAcceptedOn(LocalDate acceptedOn) {
        this.acceptedOn = acceptedOn;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getJoiningEmailPath() {
        return joiningEmailPath;
    }

    public String getJoiningEmailFileName() {
        return joiningEmailFileName;
    }

    public String getJoiningEmailContentType() {
        return joiningEmailContentType;
    }

    public Long getJoiningEmailSize() {
        return joiningEmailSize;
    }

    public boolean hasJoiningEmail() {
        return joiningEmailPath != null;
    }

    public void setJoiningEmail(String path, String fileName, String contentType, long size) {
        this.joiningEmailPath = path;
        this.joiningEmailFileName = fileName;
        this.joiningEmailContentType = contentType;
        this.joiningEmailSize = size;
    }

    public EmailStatus getOfferEmailStatus() {
        return offerEmailStatus;
    }

    public Instant getOfferEmailSentAt() {
        return offerEmailSentAt;
    }

    public String getOfferEmailError() {
        return offerEmailError;
    }

    public void recordOfferEmail(EmailStatus status, String error) {
        this.offerEmailStatus = status;
        this.offerEmailError = error;
        if (status == EmailStatus.SENT) {
            this.offerEmailSentAt = Instant.now();
        }
    }

    public boolean isOpen() {
        return status == PlacementStatus.OFFERED || status == PlacementStatus.OFFER_ACCEPTED;
    }
}
