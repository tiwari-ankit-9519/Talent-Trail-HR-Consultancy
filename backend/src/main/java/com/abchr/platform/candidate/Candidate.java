package com.abchr.platform.candidate;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate")
public class Candidate extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "current_company", length = 200)
    private String currentCompany;

    @Column(name = "current_title", length = 200)
    private String currentTitle;

    @Column(name = "total_experience_years", precision = 4, scale = 1)
    private BigDecimal totalExperienceYears;

    @Column(name = "current_ctc", precision = 12, scale = 2)
    private BigDecimal currentCtc;

    @Column(name = "expected_ctc", precision = 12, scale = 2)
    private BigDecimal expectedCtc;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Column(name = "current_location", length = 150)
    private String currentLocation;

    @Column(name = "preferred_location", length = 150)
    private String preferredLocation;

    @Column(name = "skills", columnDefinition = "text")
    private String skills;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private CandidateSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CandidateStatus status = CandidateStatus.NEW;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "resume_path", length = 500)
    private String resumePath;

    @Column(name = "resume_file_name", length = 255)
    private String resumeFileName;

    @Column(name = "resume_content_type", length = 100)
    private String resumeContentType;

    @Column(name = "resume_size")
    private Long resumeSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false, length = 20)
    private ConsentStatus consentStatus = ConsentStatus.NOT_ASKED;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_method", length = 20)
    private ConsentMethod consentMethod;

    @Column(name = "consent_recorded_at")
    private Instant consentRecordedAt;

    protected Candidate() {
    }

    public Candidate(String fullName, String email, CandidateSource source) {
        this.id = UUID.randomUUID();
        this.fullName = fullName;
        this.email = email;
        this.source = source;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }

    public BigDecimal getTotalExperienceYears() {
        return totalExperienceYears;
    }

    public void setTotalExperienceYears(BigDecimal totalExperienceYears) {
        this.totalExperienceYears = totalExperienceYears;
    }

    public BigDecimal getCurrentCtc() {
        return currentCtc;
    }

    public void setCurrentCtc(BigDecimal currentCtc) {
        this.currentCtc = currentCtc;
    }

    public BigDecimal getExpectedCtc() {
        return expectedCtc;
    }

    public void setExpectedCtc(BigDecimal expectedCtc) {
        this.expectedCtc = expectedCtc;
    }

    public Integer getNoticePeriodDays() {
        return noticePeriodDays;
    }

    public void setNoticePeriodDays(Integer noticePeriodDays) {
        this.noticePeriodDays = noticePeriodDays;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public CandidateSource getSource() {
        return source;
    }

    public void setSource(CandidateSource source) {
        this.source = source;
    }

    public CandidateStatus getStatus() {
        return status;
    }

    public void setStatus(CandidateStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getResumePath() {
        return resumePath;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public String getResumeContentType() {
        return resumeContentType;
    }

    public Long getResumeSize() {
        return resumeSize;
    }

    public boolean hasResume() {
        return resumePath != null;
    }

    public void setResume(String path, String fileName, String contentType, long size) {
        this.resumePath = path;
        this.resumeFileName = fileName;
        this.resumeContentType = contentType;
        this.resumeSize = size;
    }

    public ConsentStatus getConsentStatus() {
        return consentStatus;
    }

    public ConsentMethod getConsentMethod() {
        return consentMethod;
    }

    public Instant getConsentRecordedAt() {
        return consentRecordedAt;
    }

    public void recordConsent(ConsentStatus status, ConsentMethod method, Instant at) {
        this.consentStatus = status;
        this.consentMethod = method;
        this.consentRecordedAt = at;
    }
}
