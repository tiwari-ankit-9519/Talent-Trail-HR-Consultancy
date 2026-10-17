package com.abchr.platform.job;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "job")
public class Job extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, updatable = false, length = 20)
    private String code;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "skills", columnDefinition = "text")
    private String skills;

    @Column(name = "min_experience_years", precision = 4, scale = 1)
    private BigDecimal minExperienceYears;

    @Column(name = "max_experience_years", precision = 4, scale = 1)
    private BigDecimal maxExperienceYears;

    @Column(name = "min_ctc", precision = 12, scale = 2)
    private BigDecimal minCtc;

    @Column(name = "max_ctc", precision = 12, scale = 2)
    private BigDecimal maxCtc;

    @Column(name = "openings", nullable = false)
    private int openings = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    protected Job() {
    }

    public Job(String code, UUID clientId, String title) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.clientId = clientId;
        this.title = title;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public BigDecimal getMinExperienceYears() {
        return minExperienceYears;
    }

    public void setMinExperienceYears(BigDecimal minExperienceYears) {
        this.minExperienceYears = minExperienceYears;
    }

    public BigDecimal getMaxExperienceYears() {
        return maxExperienceYears;
    }

    public void setMaxExperienceYears(BigDecimal maxExperienceYears) {
        this.maxExperienceYears = maxExperienceYears;
    }

    public BigDecimal getMinCtc() {
        return minCtc;
    }

    public void setMinCtc(BigDecimal minCtc) {
        this.minCtc = minCtc;
    }

    public BigDecimal getMaxCtc() {
        return maxCtc;
    }

    public void setMaxCtc(BigDecimal maxCtc) {
        this.maxCtc = maxCtc;
    }

    public int getOpenings() {
        return openings;
    }

    public void setOpenings(int openings) {
        this.openings = openings;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public LocalDate getClosingDate() {
        return closingDate;
    }

    public void setClosingDate(LocalDate closingDate) {
        this.closingDate = closingDate;
    }

    public boolean acceptsApplications(LocalDate today) {
        return status == JobStatus.OPEN && published && (closingDate == null || !closingDate.isBefore(today));
    }
}
