package com.abchr.platform.placement;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.abchr.platform.replacement.ReplacementStatus;
import com.abchr.platform.shared.mail.EmailStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class PlacementDtos {

    private PlacementDtos() {
    }

    public record OfferRequest(
            @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal offeredCtc,
            @NotNull LocalDate offerDate,
            @NotNull LocalDate expectedJoiningDate,
            @Size(max = 2000) String notes,
            boolean sendCandidateEmail,
            Long version) {
    }

    public record AcceptRequest(LocalDate acceptedOn, Long version) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 500) String reason, Long version) {
    }

    public record JoinedRequest(@NotNull LocalDate joiningDate, Long version) {
    }

    public record ExitInfo(ReplacementStatus status, LocalDate exitDate, String replacedByName) {
    }

    public record ReplacementOf(String candidateName, LocalDate exitDate) {
    }

    public record PlacementSummary(
            UUID id,
            UUID submissionId,
            UUID jobId,
            String jobCode,
            String jobTitle,
            UUID clientId,
            String clientName,
            UUID candidateId,
            String candidateName,
            PlacementStatus status,
            BigDecimal offeredCtc,
            LocalDate expectedJoiningDate,
            LocalDate joiningDate,
            boolean joiningEmailAttached,
            ExitInfo exit,
            ReplacementOf replacementOf) {
    }

    public record PlacementDetail(
            UUID id,
            UUID submissionId,
            UUID jobId,
            String jobCode,
            String jobTitle,
            UUID clientId,
            String clientName,
            UUID candidateId,
            String candidateName,
            PlacementStatus status,
            BigDecimal offeredCtc,
            LocalDate offerDate,
            LocalDate expectedJoiningDate,
            LocalDate acceptedOn,
            String closeReason,
            LocalDate joiningDate,
            boolean joiningEmailAttached,
            String joiningEmailFileName,
            Long joiningEmailSize,
            String notes,
            EmailStatus offerEmailStatus,
            Instant offerEmailSentAt,
            String offerEmailError,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
    }
}
