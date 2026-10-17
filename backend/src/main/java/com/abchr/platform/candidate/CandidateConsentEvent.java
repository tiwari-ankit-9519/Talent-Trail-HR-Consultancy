package com.abchr.platform.candidate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_consent_event")
public class CandidateConsentEvent {

    @Id
    private UUID id;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false, length = 20)
    private ConsentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, updatable = false, length = 20)
    private ConsentMethod method;

    @Column(name = "note", updatable = false, length = 500)
    private String note;

    @Column(name = "recorded_by", nullable = false, updatable = false, length = 255)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected CandidateConsentEvent() {
    }

    public CandidateConsentEvent(
            UUID candidateId, ConsentStatus status, ConsentMethod method, String note, String recordedBy, Instant at) {
        this.id = UUID.randomUUID();
        this.candidateId = candidateId;
        this.status = status;
        this.method = method;
        this.note = note;
        this.recordedBy = recordedBy;
        this.recordedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public ConsentMethod getMethod() {
        return method;
    }

    public String getNote() {
        return note;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
