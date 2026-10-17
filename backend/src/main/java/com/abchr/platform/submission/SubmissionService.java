package com.abchr.platform.submission;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.candidate.Candidate;
import com.abchr.platform.candidate.CandidateRepository;
import com.abchr.platform.candidate.CandidateStatus;
import com.abchr.platform.candidate.ConsentStatus;
import com.abchr.platform.candidate.JobApplication;
import com.abchr.platform.candidate.JobApplicationRepository;
import com.abchr.platform.candidate.ResumeStorage;
import com.abchr.platform.client.Client;
import com.abchr.platform.client.ClientContact;
import com.abchr.platform.client.ClientContactRepository;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.client.ClientStatus;
import com.abchr.platform.job.Job;
import com.abchr.platform.job.JobRepository;
import com.abchr.platform.job.JobStatus;
import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.mail.EmailHandler;
import com.abchr.platform.shared.mail.EmailQueue;
import com.abchr.platform.shared.mail.MailService;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import com.abchr.platform.submission.SubmissionDtos.ContactOption;
import com.abchr.platform.submission.SubmissionDtos.FeedbackRequest;
import com.abchr.platform.submission.SubmissionDtos.FeedbackResponse;
import com.abchr.platform.submission.SubmissionDtos.InterviewResponse;
import com.abchr.platform.submission.SubmissionDtos.SubmissionDetail;
import com.abchr.platform.submission.SubmissionDtos.SubmissionSummary;
import com.abchr.platform.submission.SubmissionDtos.SubmitRequest;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SubmissionService implements EmailHandler {

    private static final Logger LOG = LogManager.getLogger(SubmissionService.class);
    private static final String EMAIL_KIND = "SUBMISSION";

    private final SubmissionRepository submissionRepository;
    private final InterviewRepository interviewRepository;
    private final SubmissionFeedbackRepository feedbackRepository;
    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final CandidateRepository candidateRepository;
    private final JobApplicationRepository applicationRepository;
    private final ResumeStorage resumeStorage;
    private final MailService mailService;
    private final EmailQueue emailQueue;
    private final SubmissionMailer mailer;
    private final AuditService auditService;
    private final TransactionTemplate tx;
    private final ZoneId zone;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            InterviewRepository interviewRepository,
            SubmissionFeedbackRepository feedbackRepository,
            JobRepository jobRepository,
            ClientRepository clientRepository,
            ClientContactRepository contactRepository,
            CandidateRepository candidateRepository,
            JobApplicationRepository applicationRepository,
            ResumeStorage resumeStorage,
            MailService mailService,
            EmailQueue emailQueue,
            SubmissionMailer mailer,
            AuditService auditService,
            PlatformTransactionManager transactionManager,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.submissionRepository = submissionRepository;
        this.interviewRepository = interviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.jobRepository = jobRepository;
        this.clientRepository = clientRepository;
        this.contactRepository = contactRepository;
        this.candidateRepository = candidateRepository;
        this.applicationRepository = applicationRepository;
        this.resumeStorage = resumeStorage;
        this.mailService = mailService;
        this.emailQueue = emailQueue;
        this.mailer = mailer;
        this.auditService = auditService;
        this.tx = new TransactionTemplate(transactionManager);
        this.zone = ZoneId.of(zoneId);
    }

    public PageResponse<SubmissionSummary> search(
            UUID jobId, UUID candidateId, UUID clientId, SubmissionStatus status, int page, int size) {
        Specification<Submission> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (jobId != null) {
                predicates.add(cb.equal(root.get("jobId"), jobId));
            }
            if (candidateId != null) {
                predicates.add(cb.equal(root.get("candidateId"), candidateId));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Submission> result = submissionRepository.findAll(spec,
                Paging.of(page, size, Sort.by("submittedAt").descending()));
        Map<UUID, Job> jobs = byId(jobRepository.findAllById(ids(result.getContent(), Submission::getJobId)), Job::getId);
        Map<UUID, Client> clients = byId(clientRepository.findAllById(ids(result.getContent(), Submission::getClientId)),
                Client::getId);
        Map<UUID, Candidate> candidates = byId(
                candidateRepository.findAllById(ids(result.getContent(), Submission::getCandidateId)), Candidate::getId);
        return PageResponse.of(result, s -> {
            Job job = jobs.get(s.getJobId());
            Client client = clients.get(s.getClientId());
            Candidate candidate = candidates.get(s.getCandidateId());
            return new SubmissionSummary(s.getId(), s.getJobId(), job == null ? null : job.getCode(),
                    job == null ? null : job.getTitle(), s.getClientId(), client == null ? null : client.getName(),
                    s.getCandidateId(), candidate == null ? null : candidate.getFullName(), s.getStatus(),
                    s.getEmailStatus(), s.getSubmittedAt());
        });
    }

    public List<ContactOption> contactsForJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.JOB_NOT_FOUND, "Job not found"));
        return contactRepository.findByClientIdOrderByPrimaryContactDescNameAsc(job.getClientId()).stream()
                .map(c -> new ContactOption(c.getId(), c.getName(), c.getEmail(), c.getDesignation(),
                        c.isPrimaryContact()))
                .toList();
    }

    public SubmissionDetail get(UUID id) {
        return detail(find(id));
    }

    public SubmissionDetail submit(SubmitRequest request) {
        UUID id = tx.execute(status -> create(request));
        LOG.info("Submission {} created, queueing email to client", id);
        queueDelivery(id);
        return get(id);
    }

    public SubmissionDetail resend(UUID id) {
        Submission submission = find(id);
        if (submission.getEmailStatus() == EmailStatus.SENT) {
            throw ApiException.rule(ErrorCodes.SUBMISSION_EMAIL_SENT, "The email was already sent to the client");
        }
        if (submission.getStatus() == SubmissionStatus.WITHDRAWN) {
            throw ApiException.rule(ErrorCodes.SUBMISSION_WITHDRAWN, "This submission was withdrawn");
        }
        queueDelivery(id);
        return get(id);
    }

    public SubmissionDetail withdraw(UUID id) {
        tx.executeWithoutResult(status -> {
            Submission submission = find(id);
            if (submission.getStatus() == SubmissionStatus.WITHDRAWN) {
                throw ApiException.rule(ErrorCodes.SUBMISSION_WITHDRAWN, "This submission is already withdrawn");
            }
            SubmissionStatus before = submission.getStatus();
            submission.setStatus(SubmissionStatus.WITHDRAWN);
            submissionRepository.saveAndFlush(submission);
            int cancelled = 0;
            for (Interview interview : interviewRepository.findBySubmissionIdOrderByRoundNumberAsc(id)) {
                if (interview.getStatus() == InterviewStatus.SCHEDULED) {
                    interview.setStatus(InterviewStatus.CANCELLED);
                    interviewRepository.save(interview);
                    cancelled++;
                }
            }
            auditService.record(AuditActions.SUBMISSION_WITHDRAWN, AuditActions.ENTITY_SUBMISSION, id.toString(),
                    "Withdrew submission (was " + before + "), cancelled " + cancelled + " scheduled interviews",
                    Changes.create().field("status", before, SubmissionStatus.WITHDRAWN).toJson());
        });
        return get(id);
    }

    public SubmissionDetail recordFeedback(UUID id, FeedbackRequest request) {
        tx.executeWithoutResult(status -> {
            Submission submission = find(id);
            if (submission.getStatus() == SubmissionStatus.WITHDRAWN) {
                throw ApiException.rule(ErrorCodes.SUBMISSION_WITHDRAWN, "This submission was withdrawn");
            }
            LocalDate today = LocalDate.now(zone);
            LocalDate received = request.receivedOn() == null ? today : request.receivedOn();
            if (received.isAfter(today)) {
                throw ApiException.rule(ErrorCodes.FEEDBACK_DATE_FUTURE, "The feedback date cannot be in the future");
            }
            if (request.interviewId() != null
                    && interviewRepository.findByIdAndSubmissionId(request.interviewId(), id).isEmpty()) {
                throw ApiException.notFound(ErrorCodes.INTERVIEW_NOT_FOUND, "Interview not found");
            }
            String comments = request.comments() == null || request.comments().isBlank()
                    ? null : request.comments().trim();
            feedbackRepository.save(new SubmissionFeedback(id, request.interviewId(), request.decision(), comments,
                    received));
            SubmissionStatus before = submission.getStatus();
            SubmissionStatus after = SubmissionStatus.valueOf(request.decision().name());
            if (before != after) {
                submission.setStatus(after);
                submissionRepository.saveAndFlush(submission);
            }
            auditService.record(AuditActions.FEEDBACK_RECORDED, AuditActions.ENTITY_SUBMISSION, id.toString(),
                    "Client feedback " + request.decision() + " recorded (status " + before + " to " + after + ")",
                    Changes.create().field("status", before, after).toJson());
        });
        return get(id);
    }

    private UUID create(SubmitRequest request) {
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.JOB_NOT_FOUND, "Job not found"));
        if (job.getStatus() != JobStatus.OPEN) {
            throw ApiException.rule(ErrorCodes.JOB_NOT_OPEN, "Candidates can only be submitted to an open job");
        }
        Client client = clientRepository.findById(job.getClientId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.CLIENT_NOT_FOUND, "Client not found"));
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw ApiException.rule(ErrorCodes.JOB_CLIENT_INACTIVE, "The client is not active");
        }
        Candidate candidate = candidateRepository.findById(request.candidateId())
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.CANDIDATE_NOT_FOUND, "Candidate not found"));
        if (candidate.getStatus() == CandidateStatus.BLOCKED) {
            throw ApiException.rule(ErrorCodes.CANDIDATE_BLOCKED, "This candidate is blocked");
        }
        if (candidate.getConsentStatus() != ConsentStatus.GRANTED) {
            LOG.warn("Submission refused: candidate {} consent is {}", candidate.getId(), candidate.getConsentStatus());
            throw ApiException.rule(ErrorCodes.CONSENT_REQUIRED,
                    "The candidate has not given consent to share their profile");
        }
        if (submissionRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            throw ApiException.conflict(ErrorCodes.SUBMISSION_EXISTS,
                    "This candidate was already submitted for this job");
        }
        Set<UUID> contactIds = new LinkedHashSet<>(request.contactIds());
        List<String> emails = new ArrayList<>();
        for (UUID contactId : contactIds) {
            ClientContact contact = contactRepository.findByIdAndClientId(contactId, client.getId())
                    .orElseThrow(() -> ApiException.notFound(ErrorCodes.CONTACT_NOT_FOUND, "Contact not found"));
            emails.add(contact.getEmail().trim());
        }
        if (!applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            applicationRepository.save(new JobApplication(job.getId(), candidate.getId(), candidate.getSource()));
        }
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        Submission submission = new Submission(job.getId(), candidate.getId(), client.getId(),
                String.join(",", emails), note);
        submissionRepository.save(submission);
        auditService.record(AuditActions.SUBMISSION_CREATED, AuditActions.ENTITY_SUBMISSION,
                submission.getId().toString(), "Submitted " + candidate.getFullName() + " to " + client.getName()
                        + " for " + job.getCode(), null);
        return submission.getId();
    }

    private void queueDelivery(UUID id) {
        String actor = Actor.current().email();
        tx.executeWithoutResult(status -> {
            Submission fresh = find(id);
            fresh.recordEmail(EmailStatus.QUEUED, null);
            submissionRepository.saveAndFlush(fresh);
            emailQueue.enqueue(EMAIL_KIND, id, actor, List.of());
        });
    }

    @Override
    public String kind() {
        return EMAIL_KIND;
    }

    @Override
    public void handle(UUID id, List<String> args) {
        deliver(id);
    }

    @Override
    public void fail(UUID id, List<String> args, String reason) {
        tx.executeWithoutResult(status -> {
            Submission fresh = find(id);
            fresh.recordEmail(EmailStatus.FAILED, reason.length() > 480 ? reason.substring(0, 480) : reason);
            submissionRepository.saveAndFlush(fresh);
        });
    }

    private void deliver(UUID id) {
        Submission submission = find(id);
        Job job = jobRepository.findById(submission.getJobId()).orElseThrow();
        Client client = clientRepository.findById(submission.getClientId()).orElseThrow();
        Candidate candidate = candidateRepository.findById(submission.getCandidateId()).orElseThrow();
        List<String> recipients = Arrays.asList(submission.getRecipients().split(","));
        String greeting = contactRepository.findByClientIdOrderByPrimaryContactDescNameAsc(client.getId()).stream()
                .filter(c -> recipients.contains(c.getEmail().trim()))
                .map(ClientContact::getName)
                .collect(Collectors.joining(", "));
        MailService.Attachment attachment = null;
        if (candidate.hasResume()) {
            try {
                attachment = new MailService.Attachment(resumeStorage.resolve(candidate.getResumePath()),
                        candidate.getResumeFileName());
            } catch (ApiException ex) {
                LOG.warn("Resume file missing for candidate {}, sending without attachment", candidate.getId());
            }
        }
        MailService.Result result = mailService.send(recipients, mailer.submissionSubject(candidate, job),
                mailer.submissionBody(candidate, job, client, greeting.isBlank() ? "Sir/Madam" : greeting,
                        submission.getNote()),
                attachment, Actor.current().email());
        tx.executeWithoutResult(status -> {
            Submission fresh = find(id);
            fresh.recordEmail(result.status(), result.error());
            submissionRepository.saveAndFlush(fresh);
            auditService.record(result.status() == EmailStatus.SENT
                            ? AuditActions.SUBMISSION_EMAIL_SENT : AuditActions.SUBMISSION_EMAIL_FAILED,
                    AuditActions.ENTITY_SUBMISSION, id.toString(),
                    "Email to " + String.join(", ", recipients) + ": " + result.status()
                            + (result.error() == null ? "" : " (" + result.error() + ")"), null);
        });
    }

    SubmissionDetail detail(Submission s) {
        Job job = jobRepository.findById(s.getJobId()).orElseThrow();
        Client client = clientRepository.findById(s.getClientId()).orElseThrow();
        Candidate candidate = candidateRepository.findById(s.getCandidateId()).orElseThrow();
        List<InterviewResponse> interviews = interviewRepository.findBySubmissionIdOrderByRoundNumberAsc(s.getId())
                .stream().map(SubmissionService::toResponse).toList();
        List<FeedbackResponse> feedback = feedbackRepository
                .findBySubmissionIdOrderByReceivedOnDescCreatedAtDesc(s.getId()).stream()
                .map(f -> new FeedbackResponse(f.getId(), f.getInterviewId(), f.getDecision(), f.getComments(),
                        f.getReceivedOn(), f.getCreatedBy(), f.getCreatedAt()))
                .toList();
        return new SubmissionDetail(s.getId(), job.getId(), job.getCode(), job.getTitle(), client.getId(),
                client.getName(), candidate.getId(), candidate.getFullName(), candidate.getEmail(),
                candidate.getPhone(), s.getStatus(), Arrays.asList(s.getRecipients().split(",")), s.getNote(),
                s.getEmailStatus(), s.getEmailSentAt(), s.getEmailError(), s.getSubmittedAt(), s.getCreatedBy(),
                interviews, feedback, s.getVersion());
    }

    static InterviewResponse toResponse(Interview i) {
        return new InterviewResponse(i.getId(), i.getRoundNumber(), i.getScheduledAt(), i.getDurationMinutes(),
                i.getMode(), i.getLocation(), i.getInterviewerName(), i.getStatus(), i.getNotes(),
                i.getInviteStatus(), i.getInviteSentAt(), i.getInviteError(), i.getVersion());
    }

    Submission find(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.SUBMISSION_NOT_FOUND, "Submission not found"));
    }

    private static <T> List<UUID> ids(List<Submission> items, Function<Submission, UUID> getter) {
        return items.stream().map(getter).distinct().toList();
    }

    private static <T> Map<UUID, T> byId(List<T> items, Function<T, UUID> getter) {
        return items.stream().collect(Collectors.toMap(getter, Function.identity(), (a, b) -> a));
    }
}
