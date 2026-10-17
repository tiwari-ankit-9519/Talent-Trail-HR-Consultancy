package com.abchr.platform.submission;

import com.abchr.platform.shared.mail.EmailStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SubmissionDtos {

    private SubmissionDtos() {
    }

    public record SubmitRequest(
            @NotNull UUID jobId,
            @NotNull UUID candidateId,
            @NotEmpty List<UUID> contactIds,
            @Size(max = 2000) String note) {
    }

    public record InterviewRequest(
            @NotNull Instant scheduledAt,
            @NotNull @Min(15) @Max(480) Integer durationMinutes,
            @NotNull InterviewMode mode,
            @Size(max = 500) String location,
            @Size(max = 150) String interviewerName,
            @Size(max = 2000) String notes,
            boolean sendInvite,
            Long version) {
    }

    public record InterviewStatusRequest(@NotNull InterviewStatus status, Long version) {
    }

    public record FeedbackRequest(
            @NotNull FeedbackDecision decision,
            @Size(max = 4000) String comments,
            LocalDate receivedOn,
            UUID interviewId) {
    }

    public record ContactOption(UUID id, String name, String email, String designation, boolean primaryContact) {
    }

    public record SubmissionSummary(
            UUID id,
            UUID jobId,
            String jobCode,
            String jobTitle,
            UUID clientId,
            String clientName,
            UUID candidateId,
            String candidateName,
            SubmissionStatus status,
            EmailStatus emailStatus,
            Instant submittedAt) {
    }

    public record InterviewResponse(
            UUID id,
            int roundNumber,
            Instant scheduledAt,
            int durationMinutes,
            InterviewMode mode,
            String location,
            String interviewerName,
            InterviewStatus status,
            String notes,
            EmailStatus inviteStatus,
            Instant inviteSentAt,
            String inviteError,
            Long version) {
    }

    public record FeedbackResponse(
            UUID id,
            UUID interviewId,
            FeedbackDecision decision,
            String comments,
            LocalDate receivedOn,
            String recordedBy,
            Instant recordedAt) {
    }

    public record SubmissionDetail(
            UUID id,
            UUID jobId,
            String jobCode,
            String jobTitle,
            UUID clientId,
            String clientName,
            UUID candidateId,
            String candidateName,
            String candidateEmail,
            String candidatePhone,
            SubmissionStatus status,
            List<String> recipients,
            String note,
            EmailStatus emailStatus,
            Instant emailSentAt,
            String emailError,
            Instant submittedAt,
            String submittedBy,
            List<InterviewResponse> interviews,
            List<FeedbackResponse> feedback,
            Long version) {
    }

    public record InterviewListItem(
            UUID id,
            UUID submissionId,
            int roundNumber,
            Instant scheduledAt,
            int durationMinutes,
            InterviewMode mode,
            InterviewStatus status,
            String candidateName,
            String jobCode,
            String jobTitle,
            String clientName) {
    }
}
