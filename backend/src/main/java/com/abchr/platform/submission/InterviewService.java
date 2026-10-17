package com.abchr.platform.submission;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.candidate.Candidate;
import com.abchr.platform.candidate.CandidateRepository;
import com.abchr.platform.client.Client;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.job.Job;
import com.abchr.platform.job.JobRepository;
import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.mail.EmailHandler;
import com.abchr.platform.shared.mail.EmailQueue;
import com.abchr.platform.shared.mail.MailService;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import com.abchr.platform.submission.SubmissionDtos.InterviewListItem;
import com.abchr.platform.submission.SubmissionDtos.InterviewRequest;
import com.abchr.platform.submission.SubmissionDtos.InterviewResponse;
import com.abchr.platform.submission.SubmissionDtos.InterviewStatusRequest;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InterviewService implements EmailHandler {

    private static final Logger LOG = LogManager.getLogger(InterviewService.class);
    private static final String EMAIL_KIND = "INTERVIEW";

    private final SubmissionRepository submissionRepository;
    private final InterviewRepository interviewRepository;
    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final CandidateRepository candidateRepository;
    private final MailService mailService;
    private final EmailQueue emailQueue;
    private final SubmissionMailer mailer;
    private final AuditService auditService;
    private final TransactionTemplate tx;

    public InterviewService(
            SubmissionRepository submissionRepository,
            InterviewRepository interviewRepository,
            JobRepository jobRepository,
            ClientRepository clientRepository,
            CandidateRepository candidateRepository,
            MailService mailService,
            EmailQueue emailQueue,
            SubmissionMailer mailer,
            AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.submissionRepository = submissionRepository;
        this.interviewRepository = interviewRepository;
        this.jobRepository = jobRepository;
        this.clientRepository = clientRepository;
        this.candidateRepository = candidateRepository;
        this.mailService = mailService;
        this.emailQueue = emailQueue;
        this.mailer = mailer;
        this.auditService = auditService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public PageResponse<InterviewListItem> search(
            InterviewStatus status, Instant from, Instant to, int page, int size) {
        Specification<Interview> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("scheduledAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<Instant>get("scheduledAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Sort sort = status == InterviewStatus.SCHEDULED
                ? Sort.by("scheduledAt").ascending() : Sort.by("scheduledAt").descending();
        Page<Interview> result = interviewRepository.findAll(spec, Paging.of(page, size, sort));
        List<UUID> submissionIds = result.getContent().stream().map(Interview::getSubmissionId).distinct().toList();
        Map<UUID, Submission> submissions = submissionRepository.findAllById(submissionIds).stream()
                .collect(Collectors.toMap(Submission::getId, Function.identity()));
        Map<UUID, Job> jobs = jobRepository.findAllById(
                submissions.values().stream().map(Submission::getJobId).distinct().toList()).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        Map<UUID, Client> clients = clientRepository.findAllById(
                submissions.values().stream().map(Submission::getClientId).distinct().toList()).stream()
                .collect(Collectors.toMap(Client::getId, Function.identity()));
        Map<UUID, Candidate> candidates = candidateRepository.findAllById(
                submissions.values().stream().map(Submission::getCandidateId).distinct().toList()).stream()
                .collect(Collectors.toMap(Candidate::getId, Function.identity()));
        return PageResponse.of(result, i -> {
            Submission s = submissions.get(i.getSubmissionId());
            Job job = s == null ? null : jobs.get(s.getJobId());
            Client client = s == null ? null : clients.get(s.getClientId());
            Candidate candidate = s == null ? null : candidates.get(s.getCandidateId());
            return new InterviewListItem(i.getId(), i.getSubmissionId(), i.getRoundNumber(), i.getScheduledAt(),
                    i.getDurationMinutes(), i.getMode(), i.getStatus(),
                    candidate == null ? null : candidate.getFullName(), job == null ? null : job.getCode(),
                    job == null ? null : job.getTitle(), client == null ? null : client.getName());
        });
    }

    public InterviewResponse schedule(UUID submissionId, InterviewRequest request) {
        UUID id = tx.execute(status -> {
            Submission submission = findSubmission(submissionId);
            if (submission.isClosed()) {
                throw ApiException.rule(ErrorCodes.SUBMISSION_CLOSED,
                        "Interviews cannot be scheduled for a " + submission.getStatus() + " submission");
            }
            validate(request);
            Interview interview = new Interview(submissionId, interviewRepository.maxRound(submissionId) + 1);
            apply(interview, request);
            interview.recordInvite(request.sendInvite() ? EmailStatus.PENDING : EmailStatus.SKIPPED, null);
            interviewRepository.save(interview);
            SubmissionStatus before = submission.getStatus();
            if (before == SubmissionStatus.SUBMITTED || before == SubmissionStatus.SHORTLISTED
                    || before == SubmissionStatus.ON_HOLD) {
                submission.setStatus(SubmissionStatus.INTERVIEWING);
                submissionRepository.save(submission);
            }
            auditService.record(AuditActions.INTERVIEW_SCHEDULED, AuditActions.ENTITY_INTERVIEW,
                    interview.getId().toString(), "Scheduled round " + interview.getRoundNumber() + " on "
                            + interview.getScheduledAt() + " (" + interview.getMode() + ")", null);
            return interview.getId();
        });
        if (request.sendInvite()) {
            sendNotice(id, "Scheduled");
        }
        return toResponse(id);
    }

    public InterviewResponse update(UUID submissionId, UUID interviewId, InterviewRequest request) {
        boolean changedSchedule = Boolean.TRUE.equals(tx.execute(status -> {
            Interview interview = findInterview(submissionId, interviewId);
            VersionCheck.verify(request.version(), interview.getVersion(), Interview.class, interviewId);
            if (interview.getStatus() != InterviewStatus.SCHEDULED) {
                throw ApiException.rule(ErrorCodes.INTERVIEW_NOT_SCHEDULED, "Only a scheduled interview can be edited");
            }
            validate(request);
            Changes changes = Changes.create()
                    .field("scheduledAt", interview.getScheduledAt(), request.scheduledAt())
                    .field("durationMinutes", interview.getDurationMinutes(), request.durationMinutes())
                    .field("mode", interview.getMode(), request.mode())
                    .field("location", interview.getLocation(), blankToNull(request.location()))
                    .field("interviewerName", interview.getInterviewerName(), blankToNull(request.interviewerName()));
            apply(interview, request);
            interviewRepository.saveAndFlush(interview);
            if (!changes.isEmpty()) {
                auditService.record(AuditActions.INTERVIEW_UPDATED, AuditActions.ENTITY_INTERVIEW,
                        interviewId.toString(), changes.summary("Updated interview round "
                                + interview.getRoundNumber()), changes.toJson());
            }
            return !changes.isEmpty();
        }));
        if (request.sendInvite() && changedSchedule) {
            sendNotice(interviewId, "Rescheduled");
        }
        return toResponse(interviewId);
    }

    public InterviewResponse changeStatus(UUID submissionId, UUID interviewId, InterviewStatusRequest request) {
        boolean notify = Boolean.TRUE.equals(tx.execute(status -> {
            Interview interview = findInterview(submissionId, interviewId);
            VersionCheck.verify(request.version(), interview.getVersion(), Interview.class, interviewId);
            if (interview.getStatus() != InterviewStatus.SCHEDULED || request.status() == InterviewStatus.SCHEDULED) {
                throw ApiException.rule(ErrorCodes.INTERVIEW_NOT_SCHEDULED,
                        "Only a scheduled interview can be marked completed, cancelled or no-show");
            }
            InterviewStatus before = interview.getStatus();
            interview.setStatus(request.status());
            interviewRepository.saveAndFlush(interview);
            auditService.record(AuditActions.INTERVIEW_STATUS_CHANGED, AuditActions.ENTITY_INTERVIEW,
                    interviewId.toString(), "Interview round " + interview.getRoundNumber() + " moved from " + before
                            + " to " + request.status(),
                    Changes.create().field("status", before, request.status()).toJson());
            return request.status() == InterviewStatus.CANCELLED && interview.getInviteStatus() == EmailStatus.SENT;
        }));
        if (notify) {
            sendNotice(interviewId, "Cancelled");
        }
        return toResponse(interviewId);
    }

    public InterviewResponse resendInvite(UUID submissionId, UUID interviewId) {
        Interview interview = findInterview(submissionId, interviewId);
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw ApiException.rule(ErrorCodes.INTERVIEW_NOT_SCHEDULED, "Only a scheduled interview can be sent");
        }
        sendNotice(interviewId, "Scheduled");
        return toResponse(interviewId);
    }

    private void sendNotice(UUID interviewId, String kind) {
        String actor = Actor.current().email();
        tx.executeWithoutResult(status -> {
            Interview fresh = interviewRepository.findById(interviewId).orElseThrow();
            fresh.recordInvite(EmailStatus.QUEUED, null);
            interviewRepository.saveAndFlush(fresh);
            emailQueue.enqueue(EMAIL_KIND, interviewId, actor, List.of(kind));
        });
    }

    @Override
    public String kind() {
        return EMAIL_KIND;
    }

    @Override
    public void handle(UUID id, List<String> args) {
        deliverNotice(id, args.isEmpty() ? "Scheduled" : args.get(0));
    }

    @Override
    public void fail(UUID id, List<String> args, String reason) {
        tx.executeWithoutResult(status -> {
            Interview fresh = interviewRepository.findById(id).orElseThrow();
            fresh.recordInvite(EmailStatus.FAILED, reason.length() > 480 ? reason.substring(0, 480) : reason);
            interviewRepository.saveAndFlush(fresh);
        });
    }

    private void deliverNotice(UUID interviewId, String kind) {
        Interview interview = interviewRepository.findById(interviewId).orElseThrow();
        Submission submission = submissionRepository.findById(interview.getSubmissionId()).orElseThrow();
        Job job = jobRepository.findById(submission.getJobId()).orElseThrow();
        Client client = clientRepository.findById(submission.getClientId()).orElseThrow();
        Candidate candidate = candidateRepository.findById(submission.getCandidateId()).orElseThrow();
        List<String> to = new ArrayList<>(Arrays.asList(submission.getRecipients().split(",")));
        if (!to.contains(candidate.getEmail())) {
            to.add(candidate.getEmail());
        }
        MailService.Result result = mailService.send(to,
                mailer.interviewSubject(kind, candidate, job, interview.getRoundNumber()),
                mailer.interviewBody(kind, interview, candidate, job, client), null, Actor.current().email());
        tx.executeWithoutResult(status -> {
            Interview fresh = interviewRepository.findById(interviewId).orElseThrow();
            fresh.recordInvite(result.status(), result.error());
            interviewRepository.saveAndFlush(fresh);
            LOG.info("Interview {} notice \"{}\" to {}: {}", interviewId, kind, to, result.status());
        });
    }

    private InterviewResponse toResponse(UUID interviewId) {
        return SubmissionService.toResponse(interviewRepository.findById(interviewId).orElseThrow());
    }

    private void validate(InterviewRequest request) {
        if (request.scheduledAt().isBefore(Instant.now().minus(5, ChronoUnit.MINUTES))) {
            throw ApiException.rule(ErrorCodes.INTERVIEW_TIME_PAST, "The interview time must be in the future");
        }
        if (request.mode() != InterviewMode.PHONE && blankToNull(request.location()) == null) {
            throw ApiException.rule(ErrorCodes.INTERVIEW_LOCATION_REQUIRED,
                    "Please enter the meeting link or the venue for this interview");
        }
    }

    private void apply(Interview interview, InterviewRequest request) {
        interview.setScheduledAt(request.scheduledAt());
        interview.setDurationMinutes(request.durationMinutes());
        interview.setMode(request.mode());
        interview.setLocation(blankToNull(request.location()));
        interview.setInterviewerName(blankToNull(request.interviewerName()));
        interview.setNotes(blankToNull(request.notes()));
    }

    private Submission findSubmission(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.SUBMISSION_NOT_FOUND, "Submission not found"));
    }

    private Interview findInterview(UUID submissionId, UUID interviewId) {
        return interviewRepository.findByIdAndSubmissionId(interviewId, submissionId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.INTERVIEW_NOT_FOUND, "Interview not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
