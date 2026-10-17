package com.abchr.platform.job;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.candidate.JobApplicationRepository;
import com.abchr.platform.client.Client;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.client.ClientStatus;
import com.abchr.platform.job.JobDtos.JobDetail;
import com.abchr.platform.job.JobDtos.JobStatusRequest;
import com.abchr.platform.job.JobDtos.JobSummary;
import com.abchr.platform.job.JobDtos.PublicJob;
import com.abchr.platform.job.JobDtos.SaveJobRequest;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final JobApplicationRepository applicationRepository;
    private final AuditService auditService;

    public JobService(
            JobRepository jobRepository,
            ClientRepository clientRepository,
            JobApplicationRepository applicationRepository,
            AuditService auditService) {
        this.jobRepository = jobRepository;
        this.clientRepository = clientRepository;
        this.applicationRepository = applicationRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummary> search(
            String query, JobStatus status, UUID clientId, int page, int size) {
        Specification<Job> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("location")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Job> result = jobRepository.findAll(spec, Paging.of(page, size, Sort.by("createdAt").descending()));
        Map<UUID, String> clientNames = clientNames(result.getContent().stream().map(Job::getClientId).distinct().toList());
        Map<UUID, Long> counts = new HashMap<>();
        if (!result.isEmpty()) {
            applicationRepository.countByJobIds(result.getContent().stream().map(Job::getId).toList())
                    .forEach(c -> counts.put(c.getJobId(), c.getTotal()));
        }
        return PageResponse.of(result, j -> new JobSummary(
                j.getId(), j.getCode(), j.getTitle(), j.getClientId(), clientNames.get(j.getClientId()),
                j.getLocation(), j.getOpenings(), j.getStatus(), j.isPublished(), j.getClosingDate(),
                counts.getOrDefault(j.getId(), 0L), j.getUpdatedAt()));
    }

    @Transactional(readOnly = true)
    public JobDetail get(UUID id) {
        return toDetail(find(id));
    }

    @Transactional
    public JobDetail create(SaveJobRequest request) {
        Client client = activeClient(request.clientId());
        validate(request);
        String code = String.format("JOB-%04d", jobRepository.nextCodeNumber());
        Job job = new Job(code, client.getId(), request.title().trim());
        apply(job, request);
        jobRepository.save(job);
        auditService.record(AuditActions.JOB_CREATED, AuditActions.ENTITY_JOB, job.getId().toString(),
                "Created job " + code + " " + job.getTitle() + " for " + client.getName(), null);
        return toDetail(job);
    }

    @Transactional
    public JobDetail update(UUID id, SaveJobRequest request) {
        Job job = find(id);
        VersionCheck.verify(request.version(), job.getVersion(), Job.class, id);
        validate(request);
        if (!job.getClientId().equals(request.clientId())) {
            activeClient(request.clientId());
        }
        Changes changes = Changes.create()
                .field("clientId", job.getClientId(), request.clientId())
                .field("title", job.getTitle(), request.title().trim())
                .field("description", job.getDescription(), blankToNull(request.description()))
                .field("location", job.getLocation(), blankToNull(request.location()))
                .field("skills", job.getSkills(), blankToNull(request.skills()))
                .field("minExperienceYears", job.getMinExperienceYears(), request.minExperienceYears())
                .field("maxExperienceYears", job.getMaxExperienceYears(), request.maxExperienceYears())
                .field("minCtc", job.getMinCtc(), request.minCtc())
                .field("maxCtc", job.getMaxCtc(), request.maxCtc())
                .field("openings", job.getOpenings(), request.openings())
                .field("published", job.isPublished(), request.published())
                .field("closingDate", job.getClosingDate(), request.closingDate());
        job.setClientId(request.clientId());
        job.setTitle(request.title().trim());
        apply(job, request);
        jobRepository.saveAndFlush(job);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.JOB_UPDATED, AuditActions.ENTITY_JOB, id.toString(),
                    changes.summary("Updated job " + job.getCode()), changes.toJson());
        }
        return toDetail(job);
    }

    @Transactional
    public JobDetail changeStatus(UUID id, JobStatusRequest request) {
        Job job = find(id);
        JobStatus before = job.getStatus();
        if (before != request.status()) {
            if (request.status() == JobStatus.OPEN) {
                activeClient(job.getClientId());
            }
            boolean publishedBefore = job.isPublished();
            job.setStatus(request.status());
            if (request.status() == JobStatus.CLOSED) {
                job.setPublished(false);
            }
            jobRepository.saveAndFlush(job);
            Changes changes = Changes.create()
                    .field("status", before, request.status())
                    .field("published", publishedBefore, job.isPublished());
            auditService.record(AuditActions.JOB_STATUS_CHANGED, AuditActions.ENTITY_JOB, id.toString(),
                    "Job " + job.getCode() + " moved from " + before + " to " + request.status(), changes.toJson());
        }
        return toDetail(job);
    }

    @Transactional(readOnly = true)
    public List<PublicJob> publicJobs() {
        LocalDate today = LocalDate.now();
        Specification<Job> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("status"), JobStatus.OPEN),
                cb.isTrue(root.get("published")),
                cb.or(cb.isNull(root.get("closingDate")), cb.greaterThanOrEqualTo(root.get("closingDate"), today)));
        return jobRepository.findAll(spec, Sort.by("createdAt").descending()).stream().map(this::toPublic).toList();
    }

    @Transactional(readOnly = true)
    public PublicJob publicJob(String code) {
        Job job = jobRepository.findByCodeIgnoreCase(code)
                .filter(j -> j.acceptsApplications(LocalDate.now()))
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.JOB_NOT_FOUND, "This job is not open for applications"));
        return toPublic(job);
    }

    @Transactional(readOnly = true)
    public Job requireAcceptingByCode(String code) {
        return jobRepository.findByCodeIgnoreCase(code)
                .filter(j -> j.acceptsApplications(LocalDate.now()))
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.JOB_NOT_FOUND, "This job is not open for applications"));
    }

    private Client activeClient(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.CLIENT_NOT_FOUND, "Client not found"));
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw ApiException.rule(ErrorCodes.JOB_CLIENT_INACTIVE, "The client is inactive");
        }
        return client;
    }

    private void validate(SaveJobRequest r) {
        nonNegative(r.minExperienceYears(), "Minimum experience");
        nonNegative(r.maxExperienceYears(), "Maximum experience");
        nonNegative(r.minCtc(), "Minimum CTC");
        nonNegative(r.maxCtc(), "Maximum CTC");
        if (r.minExperienceYears() != null && r.maxExperienceYears() != null
                && r.minExperienceYears().compareTo(r.maxExperienceYears()) > 0) {
            throw ApiException.rule(ErrorCodes.JOB_INVALID, "Minimum experience cannot exceed maximum experience");
        }
        if (r.minCtc() != null && r.maxCtc() != null && r.minCtc().compareTo(r.maxCtc()) > 0) {
            throw ApiException.rule(ErrorCodes.JOB_INVALID, "Minimum CTC cannot exceed maximum CTC");
        }
    }

    private void nonNegative(BigDecimal value, String label) {
        if (value != null && value.signum() < 0) {
            throw ApiException.rule(ErrorCodes.JOB_INVALID, label + " cannot be negative");
        }
    }

    private void apply(Job job, SaveJobRequest r) {
        job.setDescription(blankToNull(r.description()));
        job.setLocation(blankToNull(r.location()));
        job.setSkills(blankToNull(r.skills()));
        job.setMinExperienceYears(r.minExperienceYears());
        job.setMaxExperienceYears(r.maxExperienceYears());
        job.setMinCtc(r.minCtc());
        job.setMaxCtc(r.maxCtc());
        job.setOpenings(r.openings());
        job.setPublished(r.published());
        job.setClosingDate(r.closingDate());
    }

    private Job find(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.JOB_NOT_FOUND, "Job not found"));
    }

    private Map<UUID, String> clientNames(List<UUID> ids) {
        return clientRepository.findAllById(ids).stream().collect(Collectors.toMap(Client::getId, Client::getName));
    }

    private JobDetail toDetail(Job j) {
        String clientName = clientNames(List.of(j.getClientId())).get(j.getClientId());
        return new JobDetail(j.getId(), j.getCode(), j.getClientId(), clientName, j.getTitle(), j.getDescription(),
                j.getLocation(), j.getSkills(), j.getMinExperienceYears(), j.getMaxExperienceYears(), j.getMinCtc(),
                j.getMaxCtc(), j.getOpenings(), j.getStatus(), j.isPublished(), j.getClosingDate(),
                applicationRepository.countByJobId(j.getId()), j.getVersion(), j.getCreatedAt(), j.getUpdatedAt());
    }

    private PublicJob toPublic(Job j) {
        return new PublicJob(j.getCode(), j.getTitle(), j.getDescription(), j.getLocation(), j.getSkills(),
                j.getMinExperienceYears(), j.getMaxExperienceYears(), j.getOpenings(), j.getClosingDate());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
