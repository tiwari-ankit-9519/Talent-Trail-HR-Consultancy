package com.abchr.platform.replacement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public final class ReplacementDtos {

    private ReplacementDtos() {
    }

    public record ExitRequest(
            @NotNull LocalDate exitDate,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record LinkRequest(@NotNull UUID placementId, Long version) {
    }

    public record CloseRequest(@NotBlank @Size(max = 500) String note, Long version) {
    }

    public record ReplacementView(
            UUID id,
            UUID placementId,
            UUID clientId,
            String clientName,
            UUID jobId,
            String jobCode,
            String jobTitle,
            UUID candidateId,
            String candidateName,
            LocalDate joiningDate,
            LocalDate exitDate,
            String exitReason,
            int periodDays,
            LocalDate freeUntil,
            boolean withinPeriod,
            ReplacementStatus status,
            UUID replacementPlacementId,
            String replacementCandidateName,
            String closeNote,
            Long version) {
    }

    public record PlacementReplacement(ReplacementView asOriginal, ReplacementView asReplacement) {
    }
}
