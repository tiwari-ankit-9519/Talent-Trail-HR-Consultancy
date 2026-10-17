package com.abchr.platform.candidate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CandidateDtos {

    private CandidateDtos() {
    }

    public record SaveCandidateRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 30) String phone,
            @Size(max = 200) String currentCompany,
            @Size(max = 200) String currentTitle,
            BigDecimal totalExperienceYears,
            BigDecimal currentCtc,
            BigDecimal expectedCtc,
            @Min(0) @Max(365) Integer noticePeriodDays,
            @Size(max = 150) String currentLocation,
            @Size(max = 150) String preferredLocation,
            @Size(max = 2000) String skills,
            @NotNull CandidateSource source,
            @Size(max = 5000) String notes,
            Long version) {
    }

    public record CandidateStatusRequest(@NotNull CandidateStatus status) {
    }

    public record ConsentRequest(
            @NotNull ConsentStatus status,
            @NotNull ConsentMethod method,
            @Size(max = 500) String note) {
    }

    public record LinkJobRequest(@NotNull UUID jobId) {
    }

    public record CandidateSummary(
            UUID id,
            String fullName,
            String email,
            String phone,
            String currentTitle,
            String currentCompany,
            BigDecimal totalExperienceYears,
            CandidateSource source,
            CandidateStatus status,
            ConsentStatus consentStatus,
            boolean hasResume,
            Instant createdAt) {
    }

    public record ConsentEventResponse(
            ConsentStatus status, ConsentMethod method, String note, String recordedBy, Instant recordedAt) {
    }

    public record ApplicationResponse(
            UUID id,
            UUID jobId,
            String jobCode,
            String jobTitle,
            String clientName,
            CandidateSource source,
            Instant appliedAt) {
    }

    public record CandidateDetail(
            UUID id,
            String fullName,
            String email,
            String phone,
            String currentCompany,
            String currentTitle,
            BigDecimal totalExperienceYears,
            BigDecimal currentCtc,
            BigDecimal expectedCtc,
            Integer noticePeriodDays,
            String currentLocation,
            String preferredLocation,
            String skills,
            CandidateSource source,
            CandidateStatus status,
            String notes,
            boolean hasResume,
            String resumeFileName,
            Long resumeSize,
            ConsentStatus consentStatus,
            ConsentMethod consentMethod,
            Instant consentRecordedAt,
            List<ConsentEventResponse> consentHistory,
            List<ApplicationResponse> applications,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
    }
}
