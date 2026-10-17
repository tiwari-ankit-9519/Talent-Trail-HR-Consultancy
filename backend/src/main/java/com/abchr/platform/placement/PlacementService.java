package com.abchr.platform.placement;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.candidate.Candidate;
import com.abchr.platform.candidate.CandidateRepository;
import com.abchr.platform.candidate.CandidateStatus;
import com.abchr.platform.commission.CommissionService;
import com.abchr.platform.client.Client;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.job.Job;
import com.abchr.platform.job.JobRepository;
import com.abchr.platform.placement.PlacementDtos.AcceptRequest;
import com.abchr.platform.placement.PlacementDtos.JoinedRequest;
import com.abchr.platform.placement.PlacementDtos.OfferRequest;
import com.abchr.platform.placement.PlacementDtos.PlacementDetail;
import com.abchr.platform.placement.PlacementDtos.PlacementSummary;
import com.abchr.platform.placement.PlacementDtos.ReasonRequest;
import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import com.abchr.platform.submission.Submission;
import com.abchr.platform.submission.SubmissionRepository;
import com.abchr.platform.submission.SubmissionStatus;
import jakarta.persistence.criteria.Predicate;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PlacementService {

    private static final Logger LOG = LogManager.getLogger(PlacementService.class);

    public record JoiningEmailFile(Path path, String fileName, String contentType) {
    }

    private final PlacementRepository placementRepository;
    private final SubmissionRepository submissionRepository;
    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final CandidateRepository candidateRepository;
    private final JoiningEmailStorage storage;
    private final AuditService auditService;
    private final CommissionService commissionService;
    private final ZoneId zone;

    public PlacementService(
            PlacementRepository placementRepository,
            SubmissionRepository submissionRepository,
            JobRepository jobRepository,
            ClientRepository clientRepository,
            CandidateRepository candidateRepository,
            JoiningEmailStorage storage,
            AuditService auditService,
            CommissionService commissionService,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.placementRepository = placementRepository;
        this.submissionRepository = submissionRepository;
        this.jobRepository = jobRepository;
        this.clientRepository = clientRepository;
        this.candidateRepository = candidateRepository;
        this.storage = storage;
        this.auditService = auditService;
        this.commissionService = commissionService;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlacementSummary> search(
            PlacementStatus status, UUID submissionId, UUID jobId, UUID clientId, UUID candidateId, int page,
            int size) {
        Specification<Placement> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (submissionId != null) {
                predicates.add(cb.equal(root.get("submissionId"), submissionId));
            }
            if (jobId != null) {
                predicates.add(cb.equal(root.get("jobId"), jobId));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }
            if (candidateId != null) {
                predicates.add(cb.equal(root.get("candidateId"), candidateId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Placement> result = placementRepository.findAll(spec,
                Paging.of(page, size, Sort.by("createdAt").descending()));
        Map<UUID, Job> jobs = byId(jobRepository.findAllById(
                result.getContent().stream().map(Placement::getJobId).distinct().toList()), Job::getId);
        Map<UUID, Client> clients = byId(clientRepository.findAllById(
                result.getContent().stream().map(Placement::getClientId).distinct().toList()), Client::getId);
        Map<UUID, Candidate> candidates = byId(candidateRepository.findAllById(
                result.getContent().stream().map(Placement::getCandidateId).distinct().toList()), Candidate::getId);
        return PageResponse.of(result, p -> {
            Job job = jobs.get(p.getJobId());
            Client client = clients.get(p.getClientId());
            Candidate candidate = candidates.get(p.getCandidateId());
            return new PlacementSummary(p.getId(), p.getSubmissionId(), p.getJobId(),
                    job == null ? null : job.getCode(), job == null ? null : job.getTitle(), p.getClientId(),
                    client == null ? null : client.getName(), p.getCandidateId(),
                    candidate == null ? null : candidate.getFullName(), p.getStatus(), p.getOfferedCtc(),
                    p.getExpectedJoiningDate(), p.getJoiningDate(), p.hasJoiningEmail());
        });
    }

    @Transactional(readOnly = true)
    public PlacementDetail get(UUID id) {
        return toDetail(find(id));
    }

    @Transactional
    public PlacementDetail createOffer(UUID submissionId, OfferRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.SUBMISSION_NOT_FOUND, "Submission not found"));
        if (submission.getStatus() != SubmissionStatus.SELECTED) {
            throw ApiException.rule(ErrorCodes.SUBMISSION_NOT_SELECTED,
                    "An offer can only be recorded after the client has selected the candidate");
        }
        if (placementRepository.existsBySubmissionId(submissionId)) {
            throw ApiException.conflict(ErrorCodes.PLACEMENT_EXISTS, "An offer is already recorded for this submission");
        }
        validateOffer(request);
        Placement placement = new Placement(submissionId, submission.getJobId(), submission.getCandidateId(),
                submission.getClientId());
        applyOffer(placement, request);
        placement.recordOfferEmail(request.sendCandidateEmail() ? EmailStatus.PENDING : EmailStatus.SKIPPED, null);
        placementRepository.save(placement);
        auditService.record(AuditActions.OFFER_RECORDED, AuditActions.ENTITY_PLACEMENT, placement.getId().toString(),
                "Recorded offer for " + candidateName(placement) + " (" + jobCode(placement) + "), expected joining "
                        + placement.getExpectedJoiningDate(), null);
        return toDetail(placement);
    }

    @Transactional
    public PlacementDetail updateOffer(UUID id, OfferRequest request) {
        Placement placement = find(id);
        VersionCheck.verify(request.version(), placement.getVersion(), Placement.class, id);
        requireOpen(placement);
        validateOffer(request);
        Changes changes = Changes.create()
                .field("offeredCtc", placement.getOfferedCtc(), request.offeredCtc())
                .field("offerDate", placement.getOfferDate(), request.offerDate())
                .field("expectedJoiningDate", placement.getExpectedJoiningDate(), request.expectedJoiningDate());
        applyOffer(placement, request);
        placementRepository.saveAndFlush(placement);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.OFFER_UPDATED, AuditActions.ENTITY_PLACEMENT, id.toString(),
                    changes.summary("Updated offer for " + candidateName(placement)), changes.toJson());
        }
        return toDetail(placement);
    }

    @Transactional
    public PlacementDetail accept(UUID id, AcceptRequest request) {
        Placement placement = find(id);
        VersionCheck.verify(request.version(), placement.getVersion(), Placement.class, id);
        if (placement.getStatus() != PlacementStatus.OFFERED) {
            throw ApiException.rule(ErrorCodes.OFFER_NOT_OPEN, "Only an offer awaiting an answer can be accepted");
        }
        LocalDate today = LocalDate.now(zone);
        LocalDate accepted = request.acceptedOn() == null ? today : request.acceptedOn();
        if (accepted.isAfter(today) || accepted.isBefore(placement.getOfferDate())) {
            throw ApiException.rule(ErrorCodes.ACCEPTED_DATE_INVALID,
                    "The acceptance date must be between the offer date and today");
        }
        placement.setAcceptedOn(accepted);
        placement.setStatus(PlacementStatus.OFFER_ACCEPTED);
        placementRepository.saveAndFlush(placement);
        auditService.record(AuditActions.OFFER_ACCEPTED, AuditActions.ENTITY_PLACEMENT, id.toString(),
                candidateName(placement) + " accepted the offer on " + accepted,
                Changes.create().field("status", PlacementStatus.OFFERED, PlacementStatus.OFFER_ACCEPTED).toJson());
        return toDetail(placement);
    }

    @Transactional
    public PlacementDetail decline(UUID id, ReasonRequest request) {
        Placement placement = find(id);
        VersionCheck.verify(request.version(), placement.getVersion(), Placement.class, id);
        if (placement.getStatus() != PlacementStatus.OFFERED) {
            throw ApiException.rule(ErrorCodes.OFFER_NOT_OPEN, "Only an offer awaiting an answer can be declined");
        }
        placement.setStatus(PlacementStatus.OFFER_DECLINED);
        placement.setCloseReason(request.reason().trim());
        placementRepository.saveAndFlush(placement);
        auditService.record(AuditActions.OFFER_DECLINED, AuditActions.ENTITY_PLACEMENT, id.toString(),
                candidateName(placement) + " declined the offer: " + placement.getCloseReason(),
                Changes.create().field("status", PlacementStatus.OFFERED, PlacementStatus.OFFER_DECLINED).toJson());
        return toDetail(placement);
    }

    @Transactional
    public PlacementDetail markJoined(UUID id, JoinedRequest request) {
        Placement placement = find(id);
        VersionCheck.verify(request.version(), placement.getVersion(), Placement.class, id);
        if (placement.getStatus() != PlacementStatus.OFFER_ACCEPTED) {
            throw ApiException.rule(ErrorCodes.JOINING_NEEDS_ACCEPTED,
                    "Joining can only be recorded after the candidate has accepted the offer");
        }
        LocalDate today = LocalDate.now(zone);
        if (request.joiningDate().isAfter(today)) {
            throw ApiException.rule(ErrorCodes.JOINING_DATE_FUTURE, "The joining date cannot be in the future");
        }
        if (request.joiningDate().isBefore(placement.getOfferDate())) {
            throw ApiException.rule(ErrorCodes.JOINING_DATE_BEFORE_OFFER,
                    "The joining date cannot be before the offer date");
        }
        placement.setJoiningDate(request.joiningDate());
        placement.setStatus(PlacementStatus.JOINED);
        placementRepository.saveAndFlush(placement);
        candidateRepository.findById(placement.getCandidateId()).ifPresent(candidate -> {
            if (candidate.getStatus() != CandidateStatus.PLACED) {
                candidate.setStatus(CandidateStatus.PLACED);
                candidateRepository.save(candidate);
            }
        });
        commissionService.createForJoining(placement);
        auditService.record(AuditActions.JOINING_RECORDED, AuditActions.ENTITY_PLACEMENT, id.toString(),
                candidateName(placement) + " joined on " + request.joiningDate()
                        + ". The joining email is still needed before invoicing.",
                Changes.create().field("status", PlacementStatus.OFFER_ACCEPTED, PlacementStatus.JOINED).toJson());
        return toDetail(placement);
    }

    @Transactional
    public PlacementDetail markNotJoined(UUID id, ReasonRequest request) {
        Placement placement = find(id);
        VersionCheck.verify(request.version(), placement.getVersion(), Placement.class, id);
        if (placement.getStatus() != PlacementStatus.OFFER_ACCEPTED && placement.getStatus() != PlacementStatus.OFFERED) {
            throw ApiException.rule(ErrorCodes.NOT_JOINED_NOT_ALLOWED,
                    "Only an open offer can be marked as not joined");
        }
        PlacementStatus before = placement.getStatus();
        placement.setStatus(PlacementStatus.NOT_JOINED);
        placement.setCloseReason(request.reason().trim());
        placementRepository.saveAndFlush(placement);
        auditService.record(AuditActions.NOT_JOINED_RECORDED, AuditActions.ENTITY_PLACEMENT, id.toString(),
                candidateName(placement) + " did not join: " + placement.getCloseReason(),
                Changes.create().field("status", before, PlacementStatus.NOT_JOINED).toJson());
        return toDetail(placement);
    }

    @Transactional
    public PlacementDetail uploadJoiningEmail(UUID id, MultipartFile file) {
        Placement placement = find(id);
        if (placement.getStatus() != PlacementStatus.JOINED) {
            throw ApiException.rule(ErrorCodes.JOINING_EMAIL_TOO_EARLY,
                    "Attach the joining email after recording that the candidate joined");
        }
        JoiningEmailStorage.Stored stored = storage.store(id, file);
        String previous = placement.getJoiningEmailPath();
        placement.setJoiningEmail(stored.path(), stored.fileName(), stored.contentType(), stored.size());
        placementRepository.saveAndFlush(placement);
        storage.delete(previous);
        auditService.record(AuditActions.JOINING_EMAIL_UPLOADED, AuditActions.ENTITY_PLACEMENT, id.toString(),
                "Attached joining email " + stored.fileName() + " for " + candidateName(placement), null);
        return toDetail(placement);
    }

    @Transactional
    public JoiningEmailFile downloadJoiningEmail(UUID id) {
        Placement placement = find(id);
        if (!placement.hasJoiningEmail()) {
            throw ApiException.notFound(ErrorCodes.JOINING_EMAIL_NOT_FOUND, "No joining email is attached");
        }
        Path path = storage.resolve(placement.getJoiningEmailPath());
        auditService.record(AuditActions.JOINING_EMAIL_DOWNLOADED, AuditActions.ENTITY_PLACEMENT, id.toString(),
                "Downloaded joining email of " + candidateName(placement), null);
        return new JoiningEmailFile(path, placement.getJoiningEmailFileName(), placement.getJoiningEmailContentType());
    }

    private void validateOffer(OfferRequest request) {
        if (request.offerDate().isAfter(LocalDate.now(zone))) {
            throw ApiException.rule(ErrorCodes.OFFER_DATE_FUTURE, "The offer date cannot be in the future");
        }
        if (request.expectedJoiningDate().isBefore(request.offerDate())) {
            throw ApiException.rule(ErrorCodes.EXPECTED_DATE_BEFORE_OFFER,
                    "The expected joining date cannot be before the offer date");
        }
    }

    private void applyOffer(Placement placement, OfferRequest request) {
        placement.setOfferedCtc(request.offeredCtc());
        placement.setOfferDate(request.offerDate());
        placement.setExpectedJoiningDate(request.expectedJoiningDate());
        placement.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
    }

    private void requireOpen(Placement placement) {
        if (!placement.isOpen()) {
            LOG.warn("Placement {} is {} and cannot be changed", placement.getId(), placement.getStatus());
            throw ApiException.rule(ErrorCodes.OFFER_NOT_OPEN, "Only an open offer can be changed");
        }
    }

    private Placement find(UUID id) {
        return placementRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.PLACEMENT_NOT_FOUND, "Offer not found"));
    }

    private String candidateName(Placement placement) {
        return candidateRepository.findById(placement.getCandidateId()).map(Candidate::getFullName).orElse("?");
    }

    private String jobCode(Placement placement) {
        return jobRepository.findById(placement.getJobId()).map(Job::getCode).orElse("?");
    }

    private PlacementDetail toDetail(Placement p) {
        Job job = jobRepository.findById(p.getJobId()).orElse(null);
        Client client = clientRepository.findById(p.getClientId()).orElse(null);
        Candidate candidate = candidateRepository.findById(p.getCandidateId()).orElse(null);
        return new PlacementDetail(p.getId(), p.getSubmissionId(), p.getJobId(), job == null ? null : job.getCode(),
                job == null ? null : job.getTitle(), p.getClientId(), client == null ? null : client.getName(),
                p.getCandidateId(), candidate == null ? null : candidate.getFullName(), p.getStatus(),
                p.getOfferedCtc(), p.getOfferDate(), p.getExpectedJoiningDate(), p.getAcceptedOn(),
                p.getCloseReason(), p.getJoiningDate(), p.hasJoiningEmail(), p.getJoiningEmailFileName(),
                p.getJoiningEmailSize(), p.getNotes(), p.getOfferEmailStatus(), p.getOfferEmailSentAt(),
                p.getOfferEmailError(), p.getVersion(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private static <T> Map<UUID, T> byId(List<T> items, Function<T, UUID> getter) {
        return items.stream().collect(Collectors.toMap(getter, Function.identity(), (a, b) -> a));
    }
}
