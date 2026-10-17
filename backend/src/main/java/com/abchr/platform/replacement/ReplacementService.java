package com.abchr.platform.replacement;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.candidate.Candidate;
import com.abchr.platform.candidate.CandidateRepository;
import com.abchr.platform.candidate.CandidateStatus;
import com.abchr.platform.client.Client;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.commission.CommissionService;
import com.abchr.platform.invoice.InvoiceRepository;
import com.abchr.platform.invoice.InvoiceStatus;
import com.abchr.platform.job.Job;
import com.abchr.platform.job.JobRepository;
import com.abchr.platform.placement.Placement;
import com.abchr.platform.placement.PlacementRepository;
import com.abchr.platform.placement.PlacementStatus;
import com.abchr.platform.replacement.ReplacementDtos.CloseRequest;
import com.abchr.platform.replacement.ReplacementDtos.ExitRequest;
import com.abchr.platform.replacement.ReplacementDtos.LinkRequest;
import com.abchr.platform.replacement.ReplacementDtos.PlacementReplacement;
import com.abchr.platform.replacement.ReplacementDtos.ReplacementView;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplacementService {

    private final ReplacementRepository repository;
    private final PlacementRepository placementRepository;
    private final ClientRepository clientRepository;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final InvoiceRepository invoiceRepository;
    private final CommissionService commissionService;
    private final AuditService auditService;
    private final ZoneId zone;

    public ReplacementService(
            ReplacementRepository repository,
            PlacementRepository placementRepository,
            ClientRepository clientRepository,
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            InvoiceRepository invoiceRepository,
            CommissionService commissionService,
            AuditService auditService,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.repository = repository;
        this.placementRepository = placementRepository;
        this.clientRepository = clientRepository;
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.invoiceRepository = invoiceRepository;
        this.commissionService = commissionService;
        this.auditService = auditService;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReplacementView> search(ReplacementStatus status, UUID clientId, int page, int size) {
        Specification<Replacement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Replacement> result = repository.findAll(spec, Paging.of(page, size, Sort.by(Sort.Order.desc("exitDate"))));
        return PageResponse.of(result, this::toView);
    }

    @Transactional(readOnly = true)
    public PlacementReplacement forPlacement(UUID placementId) {
        ReplacementView original = repository.findByPlacementId(placementId).map(this::toView).orElse(null);
        ReplacementView replacement = repository.findByReplacementPlacementId(placementId).map(this::toView).orElse(null);
        return new PlacementReplacement(original, replacement);
    }

    @Transactional
    public ReplacementView recordExit(UUID placementId, ExitRequest request) {
        Placement placement = placementRepository.findById(placementId).orElseThrow(
                () -> ApiException.notFound(ErrorCodes.PLACEMENT_NOT_FOUND, "Placement not found"));
        if (placement.getStatus() != PlacementStatus.JOINED || placement.getJoiningDate() == null) {
            throw ApiException.rule(ErrorCodes.EXIT_NEEDS_JOINED, "An exit can only be recorded for a candidate who joined");
        }
        if (repository.findByPlacementId(placementId).isPresent()) {
            throw ApiException.conflict(ErrorCodes.EXIT_EXISTS, "An exit is already recorded for this joining");
        }
        LocalDate joining = placement.getJoiningDate();
        if (request.exitDate().isBefore(joining) || request.exitDate().isAfter(LocalDate.now(zone))) {
            throw ApiException.rule(ErrorCodes.EXIT_DATE_INVALID,
                    "The exit date must be between the joining date and today");
        }
        Client client = clientRepository.findById(placement.getClientId()).orElseThrow();
        int days = client.getReplacementPeriodDays();
        LocalDate freeUntil = joining.plusDays(days);
        boolean within = !request.exitDate().isAfter(freeUntil);

        Replacement r = Replacement.create();
        r.placementId = placementId;
        r.clientId = placement.getClientId();
        r.jobId = placement.getJobId();
        r.candidateId = placement.getCandidateId();
        r.joiningDate = joining;
        r.exitDate = request.exitDate();
        r.exitReason = request.reason().trim();
        r.periodDays = days;
        r.freeUntil = freeUntil;
        r.withinPeriod = within;
        r.status = within ? ReplacementStatus.OPEN : ReplacementStatus.NOT_ELIGIBLE;
        repository.saveAndFlush(r);

        candidateRepository.findById(placement.getCandidateId()).ifPresent(candidate -> {
            if (candidate.getStatus() == CandidateStatus.PLACED) {
                candidate.setStatus(CandidateStatus.AVAILABLE);
                candidateRepository.save(candidate);
            }
        });
        if (within) {
            commissionService.cancelForExit(placementId, "Candidate left within the replacement period");
        }
        auditService.record(AuditActions.EXIT_RECORDED, AuditActions.ENTITY_PLACEMENT, placementId.toString(),
                candidateName(r.candidateId) + " left on " + r.exitDate + (within
                        ? ", inside the " + days + " day replacement period. A free replacement is open."
                        : ", after the " + days + " day replacement period. No free replacement is due."),
                Changes.create().field("exitDate", null, r.exitDate).field("withinPeriod", null, within).toJson());
        return toView(r);
    }

    @Transactional
    public ReplacementView link(UUID id, LinkRequest request) {
        Replacement r = find(id);
        VersionCheck.verify(request.version(), r.getVersion(), Replacement.class, id);
        if (r.status != ReplacementStatus.OPEN) {
            throw ApiException.rule(ErrorCodes.REPLACEMENT_NOT_OPEN, "This replacement is not open");
        }
        Placement target = placementRepository.findById(request.placementId()).orElseThrow(
                () -> ApiException.notFound(ErrorCodes.PLACEMENT_NOT_FOUND, "Placement not found"));
        if (!target.getClientId().equals(r.clientId) || !target.getJobId().equals(r.jobId)) {
            throw ApiException.rule(ErrorCodes.REPLACEMENT_PLACEMENT_MISMATCH,
                    "The replacement must be for the same client and job");
        }
        if (target.getCandidateId().equals(r.candidateId)) {
            throw ApiException.rule(ErrorCodes.REPLACEMENT_SAME_CANDIDATE,
                    "The replacement must be a different candidate");
        }
        if (target.getStatus() == PlacementStatus.OFFER_DECLINED || target.getStatus() == PlacementStatus.NOT_JOINED) {
            throw ApiException.rule(ErrorCodes.REPLACEMENT_PLACEMENT_INVALID,
                    "This offer was declined or the candidate did not join");
        }
        if (repository.existsByReplacementPlacementId(target.getId())) {
            throw ApiException.conflict(ErrorCodes.REPLACEMENT_PLACEMENT_LINKED,
                    "This placement is already the replacement for another exit");
        }
        if (invoiceRepository.findByPlacementIdAndStatusNot(target.getId(), InvoiceStatus.CANCELLED).isPresent()) {
            throw ApiException.rule(ErrorCodes.REPLACEMENT_PLACEMENT_INVOICED,
                    "This placement already has an invoice. Cancel it first, since a replacement is free");
        }
        r.replacementPlacementId = target.getId();
        r.status = ReplacementStatus.FILLED;
        repository.saveAndFlush(r);
        auditService.record(AuditActions.REPLACEMENT_LINKED, AuditActions.ENTITY_PLACEMENT, r.placementId.toString(),
                candidateName(target.getCandidateId()) + " is the free replacement for " + candidateName(r.candidateId),
                Changes.create().field("status", ReplacementStatus.OPEN, ReplacementStatus.FILLED).toJson());
        return toView(r);
    }

    @Transactional
    public ReplacementView close(UUID id, CloseRequest request) {
        Replacement r = find(id);
        VersionCheck.verify(request.version(), r.getVersion(), Replacement.class, id);
        if (r.status != ReplacementStatus.OPEN) {
            throw ApiException.rule(ErrorCodes.REPLACEMENT_NOT_OPEN, "This replacement is not open");
        }
        r.status = ReplacementStatus.CLOSED;
        r.closeNote = request.note().trim();
        repository.saveAndFlush(r);
        auditService.record(AuditActions.REPLACEMENT_CLOSED, AuditActions.ENTITY_PLACEMENT, r.placementId.toString(),
                "Closed the replacement for " + candidateName(r.candidateId) + ": " + r.closeNote,
                Changes.create().field("status", ReplacementStatus.OPEN, ReplacementStatus.CLOSED).toJson());
        return toView(r);
    }

    private Replacement find(UUID id) {
        return repository.findById(id).orElseThrow(
                () -> ApiException.notFound(ErrorCodes.REPLACEMENT_NOT_FOUND, "Replacement not found"));
    }

    private String candidateName(UUID candidateId) {
        return candidateRepository.findById(candidateId).map(Candidate::getFullName).orElse("?");
    }

    private ReplacementView toView(Replacement r) {
        Job job = jobRepository.findById(r.jobId).orElse(null);
        String replacementName = null;
        if (r.replacementPlacementId != null) {
            replacementName = placementRepository.findById(r.replacementPlacementId)
                    .map(p -> candidateName(p.getCandidateId())).orElse(null);
        }
        return new ReplacementView(r.getId(), r.placementId, r.clientId,
                clientRepository.findById(r.clientId).map(Client::getName).orElse("?"), r.jobId,
                job == null ? null : job.getCode(), job == null ? null : job.getTitle(), r.candidateId,
                candidateName(r.candidateId), r.joiningDate, r.exitDate, r.exitReason, r.periodDays, r.freeUntil,
                r.withinPeriod, r.status, r.replacementPlacementId, replacementName, r.closeNote, r.getVersion());
    }
}
