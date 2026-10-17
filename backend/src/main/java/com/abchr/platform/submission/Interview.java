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
@Table(name = "interview")
public class Interview extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Column(name = "round_number", nullable = false, updatable = false)
    private int roundNumber;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private InterviewMode mode;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "interviewer_name", length = 150)
    private String interviewerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status", nullable = false, length = 20)
    private EmailStatus inviteStatus = EmailStatus.PENDING;

    @Column(name = "invite_sent_at")
    private Instant inviteSentAt;

    @Column(name = "invite_error", length = 500)
    private String inviteError;

    protected Interview() {
    }

    public Interview(UUID submissionId, int roundNumber) {
        this.id = UUID.randomUUID();
        this.submissionId = submissionId;
        this.roundNumber = roundNumber;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public InterviewMode getMode() {
        return mode;
    }

    public void setMode(InterviewMode mode) {
        this.mode = mode;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public EmailStatus getInviteStatus() {
        return inviteStatus;
    }

    public Instant getInviteSentAt() {
        return inviteSentAt;
    }

    public String getInviteError() {
        return inviteError;
    }

    public void recordInvite(EmailStatus status, String error) {
        this.inviteStatus = status;
        this.inviteError = error;
        if (status == EmailStatus.SENT) {
            this.inviteSentAt = Instant.now();
        }
    }
}
