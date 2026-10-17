package com.abchr.platform.job;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class JobDtos {

    private JobDtos() {
    }

    public record SaveJobRequest(
            @NotNull UUID clientId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 20000) String description,
            @Size(max = 150) String location,
            @Size(max = 2000) String skills,
            BigDecimal minExperienceYears,
            BigDecimal maxExperienceYears,
            BigDecimal minCtc,
            BigDecimal maxCtc,
            @Min(1) @Max(1000) int openings,
            boolean published,
            LocalDate closingDate,
            Long version) {
    }

    public record JobStatusRequest(@NotNull JobStatus status) {
    }

    public record JobSummary(
            UUID id,
            String code,
            String title,
            UUID clientId,
            String clientName,
            String location,
            int openings,
            JobStatus status,
            boolean published,
            LocalDate closingDate,
            long applicantCount,
            Instant updatedAt) {
    }

    public record JobDetail(
            UUID id,
            String code,
            UUID clientId,
            String clientName,
            String title,
            String description,
            String location,
            String skills,
            BigDecimal minExperienceYears,
            BigDecimal maxExperienceYears,
            BigDecimal minCtc,
            BigDecimal maxCtc,
            int openings,
            JobStatus status,
            boolean published,
            LocalDate closingDate,
            long applicantCount,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record PublicJob(
            String code,
            String title,
            String description,
            String location,
            String skills,
            BigDecimal minExperienceYears,
            BigDecimal maxExperienceYears,
            int openings,
            LocalDate closingDate) {
    }
}
