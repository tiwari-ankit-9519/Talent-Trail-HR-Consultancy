package com.abchr.platform.candidate;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.candidate.CandidateDtos.ApplicationResponse;
import com.abchr.platform.candidate.CandidateDtos.CandidateDetail;
import com.abchr.platform.candidate.CandidateDtos.CandidateStatusRequest;
import com.abchr.platform.candidate.CandidateDtos.CandidateSummary;
import com.abchr.platform.candidate.CandidateDtos.ConsentEventResponse;
import com.abchr.platform.candidate.CandidateDtos.ConsentRequest;
import com.abchr.platform.candidate.CandidateDtos.SaveCandidateRequest;
import com.abchr.platform.client.Client;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.job.Job;
import com.abchr.platform.job.JobRepository;
import com.abchr.platform.job.JobStatus;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CandidateService {

    public record ResumeFile(Path path, String fileName, String contentType) {
    }

    private final CandidateRepository candidateRepository;
    private final CandidateConsentEventRepository consentRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final ResumeStorage resumeStorage;
    private final AuditService auditService;

    public CandidateService(
            CandidateRepository candidateRepository,
            CandidateConsentEventRepository consentRepository,
            JobApplicationRepository applicationRepository,
            JobRepository jobRepository,
            ClientRepository clientRepository,
            ResumeStorage resumeStorage,
            AuditService auditService) {
        this.candidateRepository = candidateRepository;
        this.consentRepository = consentRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.clientRepository = clientRepository;
        this.resumeStorage = resumeStorage;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CandidateSummary> search(
            String query,
            CandidateStatus status,
            ConsentStatus consent,
            CandidateSource source,
            UUID jobId,
            int page,
            int size) {
        Specification<Candidate> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("phone"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("skills"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("currentCompany"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("currentTitle"), "")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (consent != null) {
                predicates.add(cb.equal(root.get("consentStatus"), consent));
            }
            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (jobId != null) {
                Subquery<Integer> sub = q.subquery(Integer.class);
                Root<JobApplication> app = sub.from(JobApplication.class);
                sub.select(cb.literal(1)).where(
                        cb.equal(app.get("candidateId"), root.get("id")), cb.equal(app.get("jobId"), jobId));
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Candidate> result = candidateRepository.findAll(spec, Paging.of(page, size, Sort.by("createdAt").descending()));
        return PageResponse.of(result, c -> new CandidateSummary(
                c.getId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getCurrentTitle(), c.getCurrentCompany(),
                c.getTotalExperienceYears(), c.getSource(), c.getStatus(), c.getConsentStatus(), c.hasResume(),
                c.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public CandidateDetail get(UUID id) {
        return toDetail(find(id));
    }

    @Transactional
    public CandidateDetail create(SaveCandidateRequest request) {
        String email = normalizeEmail(request.email());
        if (candidateRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw ApiException.conflict(ErrorCodes.CANDIDATE_EMAIL_EXISTS, "A candidate with this email already exists");
        }
        validate(request);
        Candidate candidate = new Candidate(request.fullName().trim(), email, request.source());
        apply(candidate, request);
        candidateRepository.save(candidate);
        auditService.record(AuditActions.CANDIDATE_CREATED, AuditActions.ENTITY_CANDIDATE,
                candidate.getId().toString(), "Created candidate " + candidate.getFullName()
                        + " from " + request.source(), null);
        return toDetail(candidate);
    }

    @Transactional
    public CandidateDetail update(UUID id, SaveCandidateRequest request) {
        Candidate candidate = find(id);
        VersionCheck.verify(request.version(), candidate.getVersion(), Candidate.class, id);
        String email = normalizeEmail(request.email());
        if (candidateRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflict(ErrorCodes.CANDIDATE_EMAIL_EXISTS, "A candidate with this email already exists");
        }
        validate(request);
        Changes changes = Changes.create()
                .field("fullName", candidate.getFullName(), request.fullName().trim())
                .field("email", candidate.getEmail(), email)
                .field("phone", candidate.getPhone(), blankToNull(request.phone()))
                .field("currentCompany", candidate.getCurrentCompany(), blankToNull(request.currentCompany()))
                .field("currentTitle", candidate.getCurrentTitle(), blankToNull(request.currentTitle()))
                .field("totalExperienceYears", candidate.getTotalExperienceYears(), request.totalExperienceYears())
                .field("currentCtc", candidate.getCurrentCtc(), request.currentCtc())
                .field("expectedCtc", candidate.getExpectedCtc(), request.expectedCtc())
                .field("noticePeriodDays", candidate.getNoticePeriodDays(), request.noticePeriodDays())
                .field("currentLocation", candidate.getCurrentLocation(), blankToNull(request.currentLocation()))
                .field("preferredLocation", candidate.getPreferredLocation(), blankToNull(request.preferredLocation()))
                .field("skills", candidate.getSkills(), blankToNull(request.skills()))
                .field("source", candidate.getSource(), request.source())
                .field("notes", candidate.getNotes() == null ? null : "(text)",
                        blankToNull(request.notes()) == null ? null : "(changed)");
        candidate.setFullName(request.fullName().trim());
        candidate.setEmail(email);
        candidate.setSource(request.source());
        apply(candidate, request);
        candidateRepository.saveAndFlush(candidate);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.CANDIDATE_UPDATED, AuditActions.ENTITY_CANDIDATE, id.toString(),
                    changes.summary("Updated candidate " + candidate.getFullName()), changes.toJson());
        }
        return toDetail(candidate);
    }

    @Transactional
    public CandidateDetail changeStatus(UUID id, CandidateStatusRequest request) {
        Candidate candidate = find(id);
        CandidateStatus before = candidate.getStatus();
        if (before != request.status()) {
            candidate.setStatus(request.status());
            candidateRepository.saveAndFlush(candidate);
            Changes changes = Changes.create().field("status", before, request.status());
            auditService.record(AuditActions.CANDIDATE_STATUS_CHANGED, AuditActions.ENTITY_CANDIDATE, id.toString(),
                    "Candidate " + candidate.getFullName() + " moved from " + before + " to " + request.status(),
                    changes.toJson());
        }
        return toDetail(candidate);
    }

    @Transactional
    public CandidateDetail recordConsent(UUID id, ConsentRequest request) {
        if (request.method() == ConsentMethod.APPLY_FORM) {
            throw ApiException.rule(ErrorCodes.VALIDATION_FAILED, "Consent from the apply form is recorded automatically");
        }
        Candidate candidate = find(id);
        Instant now = Instant.now();
        ConsentStatus before = candidate.getConsentStatus();
        candidate.recordConsent(request.status(), request.method(), now);
        candidateRepository.saveAndFlush(candidate);
        consentRepository.save(new CandidateConsentEvent(
                id, request.status(), request.method(), blankToNull(request.note()), Actor.current().email(), now));
        Changes changes = Changes.create()
                .field("consentStatus", before, request.status())
                .field("method", null, request.method());
        auditService.record(AuditActions.CANDIDATE_CONSENT_RECORDED, AuditActions.ENTITY_CANDIDATE, id.toString(),
                "Consent " + request.status() + " recorded by " + request.method() + " for " + candidate.getFullName(),
                changes.toJson());
        return toDetail(candidate);
    }

    @Transactional
    public CandidateDetail uploadResume(UUID id, MultipartFile file) {
        Candidate candidate = find(id);
        ResumeStorage.Stored stored = resumeStorage.store(id, file);
        String previous = candidate.getResumePath();
        candidate.setResume(stored.path(), stored.fileName(), stored.contentType(), stored.size());
        candidateRepository.saveAndFlush(candidate);
        resumeStorage.delete(previous);
        auditService.record(AuditActions.CANDIDATE_RESUME_UPLOADED, AuditActions.ENTITY_CANDIDATE, id.toString(),
                "Uploaded resume " + stored.fileName() + " for " + candidate.getFullName(), null);
        return toDetail(candidate);
    }

    @Transactional(readOnly = true)
    public ResumeFile downloadResume(UUID id) {
        Candidate candidate = find(id);
        if (!candidate.hasResume()) {
            throw ApiException.notFound(ErrorCodes.RESUME_NOT_FOUND, "This candidate has no resume");
        }
        Path path = resumeStorage.resolve(candidate.getResumePath());
        auditService.record(AuditActions.CANDIDATE_RESUME_DOWNLOADED, AuditActions.ENTITY_CANDIDATE, id.toString(),
                "Downloaded resume of " + candidate.getFullName(), null);
        return new ResumeFile(path, candidate.getResumeFileName(), candidate.getResumeContentType());
    }

    @Transactional
    public CandidateDetail linkToJob(UUID candidateId, UUID jobId) {
        Candidate candidate = find(candidateId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.JOB_NOT_FOUND, "Job not found"));
        if (candidate.getStatus() == CandidateStatus.BLOCKED) {
            throw ApiException.rule(ErrorCodes.CANDIDATE_BLOCKED, "This candidate is blocked");
        }
        if (job.getStatus() == JobStatus.CLOSED) {
            throw ApiException.rule(ErrorCodes.JOB_NOT_ACCEPTING, "This job is closed");
        }
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw ApiException.conflict(ErrorCodes.APPLICATION_EXISTS, "The candidate is already linked to this job");
        }
        applicationRepository.save(new JobApplication(jobId, candidateId, candidate.getSource()));
        auditService.record(AuditActions.CANDIDATE_LINKED_TO_JOB, AuditActions.ENTITY_CANDIDATE,
                candidateId.toString(), "Linked " + candidate.getFullName() + " to " + job.getCode(), null);
        return toDetail(candidate);
    }

    @Transactional
    public CandidateDetail unlinkFromJob(UUID candidateId, UUID applicationId) {
        Candidate candidate = find(candidateId);
        JobApplication application = applicationRepository.findById(applicationId)
                .filter(a -> a.getCandidateId().equals(candidateId))
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.APPLICATION_NOT_FOUND, "Application not found"));
        String code = jobRepository.findById(application.getJobId()).map(Job::getCode).orElse("?");
        applicationRepository.delete(application);
        auditService.record(AuditActions.CANDIDATE_UNLINKED_FROM_JOB, AuditActions.ENTITY_CANDIDATE,
                candidateId.toString(), "Unlinked " + candidate.getFullName() + " from " + code, null);
        return toDetail(candidate);
    }

    @Transactional(readOnly = true)
    public List<CandidateSummary> candidatesForJob(UUID jobId) {
        List<UUID> ids = applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId).stream()
                .map(JobApplication::getCandidateId).toList();
        Map<UUID, Candidate> byId = candidateRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Candidate::getId, c -> c));
        return ids.stream().map(byId::get).filter(c -> c != null).map(c -> new CandidateSummary(
                c.getId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getCurrentTitle(), c.getCurrentCompany(),
                c.getTotalExperienceYears(), c.getSource(), c.getStatus(), c.getConsentStatus(), c.hasResume(),
                c.getCreatedAt())).toList();
    }

    private Candidate find(UUID id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.CANDIDATE_NOT_FOUND, "Candidate not found"));
    }

    private void validate(SaveCandidateRequest r) {
        check(r.totalExperienceYears(), "Experience");
        check(r.currentCtc(), "Current CTC");
        check(r.expectedCtc(), "Expected CTC");
    }

    private void check(BigDecimal value, String label) {
        if (value != null && value.signum() < 0) {
            throw ApiException.rule(ErrorCodes.VALIDATION_FAILED, label + " cannot be negative");
        }
    }

    private void apply(Candidate c, SaveCandidateRequest r) {
        c.setPhone(blankToNull(r.phone()));
        c.setCurrentCompany(blankToNull(r.currentCompany()));
        c.setCurrentTitle(blankToNull(r.currentTitle()));
        c.setTotalExperienceYears(r.totalExperienceYears());
        c.setCurrentCtc(r.currentCtc());
        c.setExpectedCtc(r.expectedCtc());
        c.setNoticePeriodDays(r.noticePeriodDays());
        c.setCurrentLocation(blankToNull(r.currentLocation()));
        c.setPreferredLocation(blankToNull(r.preferredLocation()));
        c.setSkills(blankToNull(r.skills()));
        c.setNotes(blankToNull(r.notes()));
    }

    private CandidateDetail toDetail(Candidate c) {
        List<ConsentEventResponse> history = consentRepository.findByCandidateIdOrderByRecordedAtDesc(c.getId()).stream()
                .map(e -> new ConsentEventResponse(e.getStatus(), e.getMethod(), e.getNote(), e.getRecordedBy(),
                        e.getRecordedAt()))
                .toList();
        List<JobApplication> applications = applicationRepository.findByCandidateIdOrderByAppliedAtDesc(c.getId());
        Map<UUID, Job> jobs = jobRepository.findAllById(applications.stream().map(JobApplication::getJobId).toList())
                .stream().collect(Collectors.toMap(Job::getId, j -> j));
        Map<UUID, String> clients = clientRepository
                .findAllById(jobs.values().stream().map(Job::getClientId).distinct().toList()).stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        List<ApplicationResponse> rows = applications.stream().map(a -> {
            Job job = jobs.get(a.getJobId());
            return new ApplicationResponse(a.getId(), a.getJobId(), job == null ? null : job.getCode(),
                    job == null ? null : job.getTitle(), job == null ? null : clients.get(job.getClientId()),
                    a.getSource(), a.getAppliedAt());
        }).toList();
        return new CandidateDetail(c.getId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getCurrentCompany(),
                c.getCurrentTitle(), c.getTotalExperienceYears(), c.getCurrentCtc(), c.getExpectedCtc(),
                c.getNoticePeriodDays(), c.getCurrentLocation(), c.getPreferredLocation(), c.getSkills(),
                c.getSource(), c.getStatus(), c.getNotes(), c.hasResume(), c.getResumeFileName(), c.getResumeSize(),
                c.getConsentStatus(), c.getConsentMethod(), c.getConsentRecordedAt(), history, rows, c.getVersion(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
