package com.abchr.platform.submission;

import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.submission.SubmissionDtos.ContactOption;
import com.abchr.platform.submission.SubmissionDtos.FeedbackRequest;
import com.abchr.platform.submission.SubmissionDtos.InterviewListItem;
import com.abchr.platform.submission.SubmissionDtos.InterviewRequest;
import com.abchr.platform.submission.SubmissionDtos.InterviewResponse;
import com.abchr.platform.submission.SubmissionDtos.InterviewStatusRequest;
import com.abchr.platform.submission.SubmissionDtos.SubmissionDetail;
import com.abchr.platform.submission.SubmissionDtos.SubmissionSummary;
import com.abchr.platform.submission.SubmissionDtos.SubmitRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SubmissionController {

    private static final String VIEW = "hasAnyAuthority('SUBMISSIONS_VIEW','SUBMISSIONS_MANAGE')";
    private static final String MANAGE = "hasAuthority('SUBMISSIONS_MANAGE')";

    private final SubmissionService submissionService;
    private final InterviewService interviewService;

    public SubmissionController(SubmissionService submissionService, InterviewService interviewService) {
        this.submissionService = submissionService;
        this.interviewService = interviewService;
    }

    @GetMapping("/submissions")
    @PreAuthorize(VIEW)
    public PageResponse<SubmissionSummary> search(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return submissionService.search(jobId, candidateId, clientId, status, page, size);
    }

    @GetMapping("/submissions/contacts")
    @PreAuthorize(MANAGE)
    public List<ContactOption> contacts(@RequestParam UUID jobId) {
        return submissionService.contactsForJob(jobId);
    }

    @GetMapping("/submissions/{id}")
    @PreAuthorize(VIEW)
    public SubmissionDetail get(@PathVariable UUID id) {
        return submissionService.get(id);
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public SubmissionDetail submit(@Valid @RequestBody SubmitRequest request) {
        return submissionService.submit(request);
    }

    @PostMapping("/submissions/{id}/resend")
    @PreAuthorize(MANAGE)
    public SubmissionDetail resend(@PathVariable UUID id) {
        return submissionService.resend(id);
    }

    @PostMapping("/submissions/{id}/withdraw")
    @PreAuthorize(MANAGE)
    public SubmissionDetail withdraw(@PathVariable UUID id) {
        return submissionService.withdraw(id);
    }

    @PostMapping("/submissions/{id}/feedback")
    @PreAuthorize(MANAGE)
    public SubmissionDetail feedback(@PathVariable UUID id, @Valid @RequestBody FeedbackRequest request) {
        return submissionService.recordFeedback(id, request);
    }

    @PostMapping("/submissions/{id}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public InterviewResponse schedule(@PathVariable UUID id, @Valid @RequestBody InterviewRequest request) {
        return interviewService.schedule(id, request);
    }

    @PutMapping("/submissions/{id}/interviews/{interviewId}")
    @PreAuthorize(MANAGE)
    public InterviewResponse update(
            @PathVariable UUID id, @PathVariable UUID interviewId, @Valid @RequestBody InterviewRequest request) {
        return interviewService.update(id, interviewId, request);
    }

    @PatchMapping("/submissions/{id}/interviews/{interviewId}/status")
    @PreAuthorize(MANAGE)
    public InterviewResponse changeStatus(
            @PathVariable UUID id, @PathVariable UUID interviewId, @Valid @RequestBody InterviewStatusRequest request) {
        return interviewService.changeStatus(id, interviewId, request);
    }

    @PostMapping("/submissions/{id}/interviews/{interviewId}/resend")
    @PreAuthorize(MANAGE)
    public InterviewResponse resendInvite(@PathVariable UUID id, @PathVariable UUID interviewId) {
        return interviewService.resendInvite(id, interviewId);
    }

    @GetMapping("/interviews")
    @PreAuthorize(VIEW)
    public PageResponse<InterviewListItem> interviews(
            @RequestParam(required = false) InterviewStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return interviewService.search(status, from, to, page, size);
    }
}
