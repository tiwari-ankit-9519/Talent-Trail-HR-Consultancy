package com.abchr.platform.commission;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.candidate.Candidate;
import com.abchr.platform.candidate.CandidateRepository;
import com.abchr.platform.client.Client;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.commission.CommissionDtos.AmountRequest;
import com.abchr.platform.commission.CommissionDtos.CommissionView;
import com.abchr.platform.commission.CommissionDtos.PayRequest;
import com.abchr.platform.invoice.Invoice;
import com.abchr.platform.invoice.InvoiceRepository;
import com.abchr.platform.invoice.InvoiceStatus;
import com.abchr.platform.job.Job;
import com.abchr.platform.job.JobRepository;
import com.abchr.platform.placement.Placement;
import com.abchr.platform.placement.PlacementRepository;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import com.abchr.platform.submission.Submission;
import com.abchr.platform.submission.SubmissionRepository;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionService {

    private final CommissionRepository repository;
    private final PlacementRepository placementRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditService auditService;
    private final ZoneId zone;
    private final BigDecimal min;
    private final BigDecimal max;

    public CommissionService(
            CommissionRepository repository,
            PlacementRepository placementRepository,
            SubmissionRepository submissionRepository,
            UserRepository userRepository,
            ClientRepository clientRepository,
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            InvoiceRepository invoiceRepository,
            AuditService auditService,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId,
            @Value("${app.commission.min-amount:2000}") BigDecimal min,
            @Value("${app.commission.max-amount:5000}") BigDecimal max) {
        this.repository = repository;
        this.placementRepository = placementRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditService = auditService;
        this.zone = ZoneId.of(zoneId);
        this.min = min;
        this.max = max;
    }

    @Transactional
    public void createForJoining(Placement placement) {
        if (repository.findByPlacementId(placement.getId()).isPresent()) {
            return;
        }
        Submission submission = submissionRepository.findById(placement.getSubmissionId()).orElse(null);
        String email = submission == null ? null : submission.getCreatedBy();
        Optional<AppUser> user = email == null ? Optional.empty() : userRepository.findByEmailIgnoreCase(email);
        Client client = clientRepository.findById(placement.getClientId()).orElseThrow();
        Commission c = Commission.create();
        c.placementId = placement.getId();
        c.recruiterUserId = user.map(AppUser::getId).orElse(null);
        c.recruiterEmail = email;
        c.recruiterName = user.map(AppUser::getFullName).orElse(email);
        c.periodDays = client.getReplacementPeriodDays();
        c.payableAfter = placement.getJoiningDate().plusDays(c.periodDays);
        repository.saveAndFlush(c);
        auditService.record(AuditActions.COMMISSION_CREATED, AuditActions.ENTITY_COMMISSION, c.getId().toString(),
                "Commission opened for " + c.recruiterName + ". Payable after " + c.payableAfter, null);
    }

    @Transactional
    public void cancelForExit(UUID placementId, String reason) {
        repository.findByPlacementId(placementId).ifPresent(c -> {
            if (c.status == CommissionStatus.PENDING) {
                c.status = CommissionStatus.CANCELLED;
                c.cancelReason = reason;
                repository.saveAndFlush(c);
                auditService.record(AuditActions.COMMISSION_CANCELLED, AuditActions.ENTITY_COMMISSION,
                        c.getId().toString(), "Commission cancelled: " + reason, null);
            }
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<CommissionView> search(CommissionStatus status, UUID recruiterId, int page, int size) {
        boolean all = Permissions.hasAny("COMMISSIONS_VIEW", "COMMISSIONS_MANAGE");
        UUID own = Actor.current().id();
        LocalDate today = LocalDate.now(zone);
        Specification<Commission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!all) {
                predicates.add(own == null ? cb.disjunction() : cb.equal(root.get("recruiterUserId"), own));
            } else if (recruiterId != null) {
                predicates.add(cb.equal(root.get("recruiterUserId"), recruiterId));
            }
            if (status == CommissionStatus.PAID || status == CommissionStatus.CANCELLED) {
                predicates.add(cb.equal(root.get("status"), status));
            } else if (status != null) {
                predicates.add(cb.equal(root.get("status"), CommissionStatus.PENDING));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Commission> result = repository.findAll(spec, Paging.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
        return PageResponse.of(result, c -> toView(c, today));
    }

    @Transactional(readOnly = true)
    public CommissionView forPlacement(UUID placementId) {
        Optional<Commission> found = repository.findByPlacementId(placementId);
        if (found.isEmpty()) {
            return null;
        }
        Commission c = found.get();
        boolean all = Permissions.hasAny("COMMISSIONS_VIEW", "COMMISSIONS_MANAGE");
        if (!all && (c.recruiterUserId == null || !c.recruiterUserId.equals(Actor.current().id()))) {
            return null;
        }
        return toView(c, LocalDate.now(zone));
    }

    @Transactional
    public CommissionView setAmount(UUID id, AmountRequest request) {
        Commission c = find(id);
        VersionCheck.verify(request.version(), c.getVersion(), Commission.class, id);
        requireOpen(c);
        if (request.amount().compareTo(min) < 0 || request.amount().compareTo(max) > 0) {
            throw ApiException.rule(ErrorCodes.COMMISSION_AMOUNT_RANGE,
                    "The commission must be between " + min + " and " + max);
        }
        BigDecimal before = c.amount;
        c.amount = request.amount();
        repository.saveAndFlush(c);
        auditService.record(AuditActions.COMMISSION_AMOUNT_SET, AuditActions.ENTITY_COMMISSION, id.toString(),
                "Set commission for " + c.recruiterName + " to " + c.amount,
                Changes.create().field("amount", before, c.amount).toJson());
        return toView(c, LocalDate.now(zone));
    }

    @Transactional
    public CommissionView pay(UUID id, PayRequest request) {
        Commission c = find(id);
        VersionCheck.verify(request.version(), c.getVersion(), Commission.class, id);
        requireOpen(c);
        LocalDate today = LocalDate.now(zone);
        List<String> blockers = blockers(c, today);
        if (blockers.contains("AMOUNT_NOT_SET")) {
            throw ApiException.rule(ErrorCodes.COMMISSION_AMOUNT_MISSING, "Set the commission amount first");
        }
        if (blockers.contains("PERIOD_RUNNING")) {
            throw ApiException.rule(ErrorCodes.COMMISSION_PERIOD_RUNNING,
                    "The replacement period runs until " + c.payableAfter);
        }
        if (blockers.contains("INVOICE_UNPAID")) {
            throw ApiException.rule(ErrorCodes.COMMISSION_INVOICE_UNPAID, "The client has not paid the invoice in full");
        }
        if (request.paidOn().isAfter(today)) {
            throw ApiException.rule(ErrorCodes.COMMISSION_PAID_DATE_INVALID, "The paid date cannot be in the future");
        }
        c.status = CommissionStatus.PAID;
        c.paidOn = request.paidOn();
        c.paymentReference = request.reference() == null || request.reference().isBlank()
                ? null : request.reference().trim();
        repository.saveAndFlush(c);
        auditService.record(AuditActions.COMMISSION_PAID, AuditActions.ENTITY_COMMISSION, id.toString(),
                "Paid commission " + c.amount + " to " + c.recruiterName,
                Changes.create().field("status", CommissionStatus.PENDING, CommissionStatus.PAID).toJson());
        return toView(c, today);
    }

    public record PayableCommission(UUID id, BigDecimal amount, String label) {
    }

    @Transactional(readOnly = true)
    public List<PayableCommission> payableFor(UUID recruiterUserId) {
        LocalDate today = LocalDate.now(zone);
        List<PayableCommission> out = new ArrayList<>();
        for (Commission c : repository.findByRecruiterUserIdAndStatus(recruiterUserId, CommissionStatus.PENDING)) {
            if (!blockers(c, today).isEmpty()) {
                continue;
            }
            CommissionView view = toView(c, today);
            out.add(new PayableCommission(c.getId(), c.amount,
                    "Commission " + view.candidateName() + " (" + view.jobCode() + ")"));
        }
        return out;
    }

    @Transactional
    public void payWithSalary(UUID id, LocalDate paidOn, String reference) {
        Commission c = find(id);
        requireOpen(c);
        if (!blockers(c, LocalDate.now(zone)).isEmpty()) {
            throw ApiException.rule(ErrorCodes.COMMISSION_INVOICE_UNPAID, "This commission is no longer payable");
        }
        c.status = CommissionStatus.PAID;
        c.paidOn = paidOn;
        c.paymentReference = reference;
        repository.saveAndFlush(c);
        auditService.record(AuditActions.COMMISSION_PAID, AuditActions.ENTITY_COMMISSION, id.toString(),
                "Paid commission " + c.amount + " to " + c.recruiterName + " with salary", null);
    }

    private void requireOpen(Commission c) {
        if (c.status != CommissionStatus.PENDING) {
            throw ApiException.rule(ErrorCodes.COMMISSION_CLOSED, "This commission is already paid or cancelled");
        }
    }

    private Commission find(UUID id) {
        return repository.findById(id).orElseThrow(
                () -> ApiException.notFound(ErrorCodes.COMMISSION_NOT_FOUND, "Commission not found"));
    }

    private Optional<Invoice> invoiceOf(Commission c) {
        return invoiceRepository.findByPlacementIdAndStatusNot(c.placementId, InvoiceStatus.CANCELLED);
    }

    private List<String> blockers(Commission c, LocalDate today) {
        List<String> blockers = new ArrayList<>();
        if (c.status != CommissionStatus.PENDING) {
            return blockers;
        }
        if (c.amount == null) {
            blockers.add("AMOUNT_NOT_SET");
        }
        if (!today.isAfter(c.payableAfter)) {
            blockers.add("PERIOD_RUNNING");
        }
        Optional<Invoice> invoice = invoiceOf(c);
        if (invoice.isEmpty() || invoice.get().getStatus() != InvoiceStatus.PAID) {
            blockers.add("INVOICE_UNPAID");
        }
        return blockers;
    }

    private CommissionView toView(Commission c, LocalDate today) {
        List<String> blockers = blockers(c, today);
        CommissionStatus display = c.status == CommissionStatus.PENDING && blockers.isEmpty()
                ? CommissionStatus.PAYABLE : c.status;
        Placement placement = placementRepository.findById(c.placementId).orElse(null);
        String candidate = placement == null ? "?"
                : candidateRepository.findById(placement.getCandidateId()).map(Candidate::getFullName).orElse("?");
        String job = placement == null ? "?" : jobRepository.findById(placement.getJobId()).map(Job::getCode).orElse("?");
        String client = placement == null ? "?"
                : clientRepository.findById(placement.getClientId()).map(Client::getName).orElse("?");
        Optional<Invoice> invoice = invoiceOf(c);
        return new CommissionView(c.getId(), c.placementId, candidate, job, client, c.recruiterUserId,
                c.recruiterName, c.amount, min, max, display, c.payableAfter, c.periodDays,
                invoice.map(Invoice::getNumber).orElse(null), invoice.map(Invoice::getStatus).orElse(null), blockers,
                c.paidOn, c.paymentReference, c.cancelReason, c.getVersion());
    }
}
