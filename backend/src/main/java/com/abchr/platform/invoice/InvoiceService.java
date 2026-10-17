package com.abchr.platform.invoice;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.candidate.Candidate;
import com.abchr.platform.candidate.CandidateRepository;
import com.abchr.platform.client.Client;
import com.abchr.platform.client.ClientContactRepository;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.invoice.InvoiceDtos.CancelRequest;
import com.abchr.platform.invoice.InvoiceDtos.ContactView;
import com.abchr.platform.invoice.InvoiceDtos.InvoiceDetail;
import com.abchr.platform.invoice.InvoiceDtos.InvoiceSummary;
import com.abchr.platform.invoice.InvoiceDtos.PaymentRequest;
import com.abchr.platform.invoice.InvoiceDtos.PaymentView;
import com.abchr.platform.job.Job;
import com.abchr.platform.job.JobRepository;
import com.abchr.platform.placement.Placement;
import com.abchr.platform.placement.PlacementRepository;
import com.abchr.platform.placement.PlacementStatus;
import com.abchr.platform.replacement.ReplacementRepository;
import com.abchr.platform.settings.CompanyProfile;
import com.abchr.platform.settings.CompanyService;
import com.abchr.platform.shared.mail.EmailQueue;
import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final List<InvoiceStatus> OPEN = List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID);

    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final PlacementRepository placementRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final CompanyService companyService;
    private final ReplacementRepository replacementRepository;
    private final AuditService auditService;
    private final EmailQueue emailQueue;
    private final ZoneId zone;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoicePaymentRepository paymentRepository,
            PlacementRepository placementRepository,
            ClientRepository clientRepository,
            ClientContactRepository contactRepository,
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            CompanyService companyService,
            ReplacementRepository replacementRepository,
            AuditService auditService,
            EmailQueue emailQueue,
            @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.placementRepository = placementRepository;
        this.clientRepository = clientRepository;
        this.contactRepository = contactRepository;
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.companyService = companyService;
        this.replacementRepository = replacementRepository;
        this.auditService = auditService;
        this.emailQueue = emailQueue;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummary> search(
            InvoiceStatus status, UUID clientId, boolean overdueOnly, String q, int page, int size) {
        LocalDate today = LocalDate.now(zone);
        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }
            if (overdueOnly) {
                predicates.add(root.get("status").in(OPEN));
                predicates.add(cb.lessThan(root.<LocalDate>get("dueDate"), today));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("number")), like),
                        cb.like(cb.lower(root.get("candidateName")), like),
                        cb.like(cb.lower(root.get("buyerName")), like)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Invoice> result = invoiceRepository.findAll(spec, Paging.of(page, size,
                Sort.by(Sort.Order.desc("invoiceDate"), Sort.Order.desc("number"))));
        Map<UUID, String> clients = clientRepository.findAllById(
                result.getContent().stream().map(i -> i.clientId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        return PageResponse.of(result, i -> toSummary(i, clients.getOrDefault(i.clientId, "-"), today));
    }

    @Transactional(readOnly = true)
    public InvoiceDetail get(UUID id) {
        return toDetail(find(id));
    }

    @Transactional(readOnly = true)
    public InvoiceDetail forPlacement(UUID placementId) {
        return invoiceRepository.findByPlacementIdAndStatusNot(placementId, InvoiceStatus.CANCELLED)
                .map(this::toDetail).orElse(null);
    }

    @Transactional
    public InvoiceDetail create(UUID placementId) {
        Placement placement = placementRepository.findById(placementId).orElseThrow(
                () -> ApiException.notFound(ErrorCodes.PLACEMENT_NOT_FOUND, "Placement not found"));
        if (placement.getStatus() != PlacementStatus.JOINED || placement.getJoiningDate() == null) {
            throw ApiException.rule(ErrorCodes.INVOICE_NEEDS_JOINING,
                    "An invoice can only be raised after the candidate has joined");
        }
        if (!placement.hasJoiningEmail()) {
            throw ApiException.rule(ErrorCodes.INVOICE_NEEDS_JOINING_EMAIL,
                    "Attach the joining email before raising the invoice");
        }
        if (invoiceRepository.findByPlacementIdAndStatusNot(placementId, InvoiceStatus.CANCELLED).isPresent()) {
            throw ApiException.conflict(ErrorCodes.INVOICE_EXISTS, "An invoice already exists for this joining");
        }
        if (replacementRepository.existsByReplacementPlacementId(placementId)) {
            throw ApiException.rule(ErrorCodes.INVOICE_REPLACEMENT_FREE,
                    "This joining is a free replacement, so no invoice can be raised");
        }
        LocalDate invoiceDate = placement.getJoiningDate();
        if (invoiceDate.isAfter(LocalDate.now(zone))) {
            throw ApiException.rule(ErrorCodes.INVOICE_DATE_INVALID, "The invoice date cannot be in the future");
        }
        Client client = clientRepository.findById(placement.getClientId()).orElseThrow();
        Job job = jobRepository.findById(placement.getJobId()).orElseThrow();
        Candidate candidate = candidateRepository.findById(placement.getCandidateId()).orElseThrow();
        CompanyProfile company = companyService.load();
        requireCompany(company, client);

        BigDecimal base = InvoiceCalculator.fee(client.getFeeType(), client.getFeeValue(), placement.getOfferedCtc());
        if (base.signum() <= 0) {
            throw ApiException.rule(ErrorCodes.INVOICE_FEE_INVALID,
                    "The fee works out to zero. Check the client's fee terms and the offered CTC");
        }
        boolean sameState = sameState(company.getState(), client.getState());
        InvoiceCalculator.Amounts amounts = InvoiceCalculator.compute(base, client.isGstApplicable(),
                client.getGstRate(), sameState, client.isTdsApplicable(), client.getTdsRate());

        Invoice inv = Invoice.create();
        inv.number = nextNumber(company.getInvoicePrefix(), invoiceDate);
        inv.placementId = placement.getId();
        inv.clientId = client.getId();
        inv.jobId = job.getId();
        inv.candidateId = candidate.getId();
        inv.invoiceDate = invoiceDate;
        inv.dueDate = invoiceDate.plusDays(client.getPaymentTermsDays());
        inv.joiningDate = placement.getJoiningDate();
        inv.candidateName = candidate.getFullName();
        inv.jobCode = job.getCode();
        inv.jobTitle = job.getTitle();
        inv.description = "Recruitment service fee for placement of " + candidate.getFullName() + " as "
                + job.getTitle() + " (" + job.getCode() + ")";
        inv.sellerName = company.getLegalName();
        inv.sellerGstin = company.getGstin();
        inv.sellerPan = company.getPan();
        inv.sellerAddress = address(company.getAddressLine1(), company.getAddressLine2(), company.getCity(),
                company.getPostalCode());
        inv.sellerState = company.getState();
        inv.sellerBank = bank(company);
        inv.sacCode = company.getSacCode();
        inv.buyerName = client.getLegalName() != null && !client.getLegalName().isBlank()
                ? client.getLegalName() : client.getName();
        inv.buyerGstin = client.getGstin();
        inv.buyerAddress = address(client.getAddressLine1(), client.getAddressLine2(), client.getCity(),
                client.getPostalCode());
        inv.buyerState = client.getState();
        inv.offeredCtc = placement.getOfferedCtc();
        inv.feeType = client.getFeeType().name();
        inv.feeValue = client.getFeeValue();
        inv.baseAmount = amounts.base();
        inv.gstRate = amounts.gstRate();
        inv.cgstAmount = amounts.cgst();
        inv.sgstAmount = amounts.sgst();
        inv.igstAmount = amounts.igst();
        inv.totalAmount = amounts.total();
        inv.tdsRate = amounts.tdsRate();
        inv.tdsAmount = amounts.tds();
        inv.netReceivable = amounts.net();
        invoiceRepository.saveAndFlush(inv);
        auditService.record(AuditActions.INVOICE_CREATED, AuditActions.ENTITY_INVOICE, inv.getId().toString(),
                "Raised invoice " + inv.number + " to " + inv.buyerName + " for " + inv.totalAmount,
                Changes.create().field("number", null, inv.number).field("total", null, inv.totalAmount).toJson());
        return toDetail(inv);
    }

    @Transactional
    public InvoiceDetail cancel(UUID id, CancelRequest request) {
        Invoice inv = find(id);
        VersionCheck.verify(request.version(), inv.getVersion(), Invoice.class, id);
        if (inv.status == InvoiceStatus.CANCELLED) {
            throw ApiException.rule(ErrorCodes.INVOICE_CANCELLED, "This invoice is already cancelled");
        }
        if (inv.settledAmount.signum() > 0) {
            throw ApiException.rule(ErrorCodes.INVOICE_HAS_PAYMENTS,
                    "Remove the recorded payments before cancelling this invoice");
        }
        InvoiceStatus before = inv.status;
        inv.status = InvoiceStatus.CANCELLED;
        inv.cancelReason = request.reason().trim();
        inv.cancelledAt = Instant.now();
        invoiceRepository.saveAndFlush(inv);
        auditService.record(AuditActions.INVOICE_CANCELLED, AuditActions.ENTITY_INVOICE, id.toString(),
                "Cancelled invoice " + inv.number + ": " + inv.cancelReason,
                Changes.create().field("status", before, inv.status).toJson());
        return toDetail(inv);
    }

    @Transactional
    public InvoiceDetail addPayment(UUID id, PaymentRequest request) {
        Invoice inv = find(id);
        requireOpenForPayment(inv);
        LocalDate today = LocalDate.now(zone);
        if (request.paidOn().isAfter(today) || request.paidOn().isBefore(inv.invoiceDate)) {
            throw ApiException.rule(ErrorCodes.PAYMENT_DATE_INVALID,
                    "The payment date must be between the invoice date and today");
        }
        BigDecimal applied = request.amountReceived().add(request.tdsDeducted());
        if (applied.signum() <= 0) {
            throw ApiException.rule(ErrorCodes.PAYMENT_AMOUNT_REQUIRED, "Enter the amount received");
        }
        if (applied.compareTo(inv.getBalance()) > 0) {
            throw ApiException.rule(ErrorCodes.PAYMENT_EXCEEDS_BALANCE,
                    "The payment is more than the balance of " + inv.getBalance());
        }
        InvoicePayment payment = InvoicePayment.create();
        payment.invoiceId = id;
        payment.paidOn = request.paidOn();
        payment.amountReceived = request.amountReceived();
        payment.tdsDeducted = request.tdsDeducted();
        payment.mode = request.mode();
        payment.reference = blank(request.reference());
        payment.note = blank(request.note());
        paymentRepository.saveAndFlush(payment);
        refreshSettlement(inv);
        auditService.record(AuditActions.PAYMENT_RECORDED, AuditActions.ENTITY_INVOICE, id.toString(),
                "Recorded payment of " + request.amountReceived() + " (TDS " + request.tdsDeducted() + ") on "
                        + inv.number, null);
        return toDetail(inv);
    }

    @Transactional
    public InvoiceDetail removePayment(UUID id, UUID paymentId) {
        Invoice inv = find(id);
        requireOpenForPayment(inv);
        InvoicePayment payment = paymentRepository.findByIdAndInvoiceId(paymentId, id).orElseThrow(
                () -> ApiException.notFound(ErrorCodes.PAYMENT_NOT_FOUND, "Payment not found"));
        paymentRepository.delete(payment);
        paymentRepository.flush();
        refreshSettlement(inv);
        auditService.record(AuditActions.PAYMENT_REMOVED, AuditActions.ENTITY_INVOICE, id.toString(),
                "Removed payment of " + payment.amountReceived + " from " + inv.number, null);
        return toDetail(inv);
    }

    @Transactional(readOnly = true)
    public Invoice findForSending(UUID id) {
        Invoice inv = find(id);
        if (inv.status == InvoiceStatus.CANCELLED) {
            throw ApiException.rule(ErrorCodes.INVOICE_CANCELLED, "A cancelled invoice cannot be sent");
        }
        return inv;
    }

    @Transactional
    public void queueSend(UUID id, List<String> to, String actor) {
        Invoice inv = find(id);
        if (inv.status == InvoiceStatus.CANCELLED) {
            throw ApiException.rule(ErrorCodes.INVOICE_CANCELLED, "A cancelled invoice cannot be sent");
        }
        inv.emailStatus = EmailStatus.QUEUED;
        inv.emailError = null;
        invoiceRepository.saveAndFlush(inv);
        emailQueue.enqueue("INVOICE", id, actor, to);
    }

    @Transactional
    public void recordSend(UUID id, EmailStatus status, String error, List<String> to) {
        Invoice inv = find(id);
        inv.emailStatus = status;
        inv.emailError = error;
        if (status == EmailStatus.SENT) {
            inv.emailSentAt = Instant.now();
            inv.sentTo = String.join(", ", to);
        }
        invoiceRepository.saveAndFlush(inv);
        auditService.record(status == EmailStatus.SENT ? AuditActions.INVOICE_SENT : AuditActions.INVOICE_SEND_FAILED,
                AuditActions.ENTITY_INVOICE, id.toString(),
                (status == EmailStatus.SENT ? "Emailed " : "Could not email ") + inv.number + " to " + to, null);
    }

    @Transactional(readOnly = true)
    public InvoiceDetail detail(UUID id) {
        return toDetail(find(id));
    }

    private void refreshSettlement(Invoice inv) {
        BigDecimal settled = paymentRepository.findByInvoiceIdOrderByPaidOnAscCreatedAtAsc(inv.getId()).stream()
                .map(p -> p.amountReceived.add(p.tdsDeducted))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        inv.settledAmount = settled;
        inv.status = settled.signum() == 0 ? InvoiceStatus.ISSUED
                : settled.compareTo(inv.totalAmount) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
        invoiceRepository.saveAndFlush(inv);
    }

    private void requireOpenForPayment(Invoice inv) {
        if (inv.status == InvoiceStatus.CANCELLED) {
            throw ApiException.rule(ErrorCodes.INVOICE_CANCELLED, "This invoice is cancelled");
        }
    }

    private void requireCompany(CompanyProfile company, Client client) {
        boolean incomplete = isBlank(company.getAddressLine1()) || isBlank(company.getState())
                || isBlank(company.getBankAccountNumber()) || isBlank(company.getBankIfsc());
        if (incomplete) {
            throw ApiException.rule(ErrorCodes.COMPANY_PROFILE_INCOMPLETE,
                    "Complete the company address, state and bank details in Settings first");
        }
        if (client.isGstApplicable() && isBlank(company.getGstin())) {
            throw ApiException.rule(ErrorCodes.COMPANY_GSTIN_MISSING,
                    "Add the company GSTIN in Settings before raising a GST invoice");
        }
    }

    private String nextNumber(String prefix, LocalDate invoiceDate) {
        int year = invoiceDate.getYear();
        int start = invoiceDate.getMonthValue() >= 4 ? year : year - 1;
        String fy = start + "-" + String.format("%02d", (start + 1) % 100);
        Integer next = invoiceRepository.nextNumber(fy);
        return prefix + "/" + fy + "/" + String.format("%04d", next);
    }

    private Invoice find(UUID id) {
        return invoiceRepository.findById(id).orElseThrow(
                () -> ApiException.notFound(ErrorCodes.INVOICE_NOT_FOUND, "Invoice not found"));
    }

    private InvoiceSummary toSummary(Invoice i, String clientName, LocalDate today) {
        return new InvoiceSummary(i.getId(), i.number, i.status, isOverdue(i, today), i.clientId, clientName,
                i.candidateName, i.jobCode, i.invoiceDate, i.dueDate, i.totalAmount, i.settledAmount, i.getBalance(),
                i.emailStatus);
    }

    private InvoiceDetail toDetail(Invoice i) {
        LocalDate today = LocalDate.now(zone);
        String clientName = clientRepository.findById(i.clientId).map(Client::getName).orElse(i.buyerName);
        List<PaymentView> payments = paymentRepository.findByInvoiceIdOrderByPaidOnAscCreatedAtAsc(i.getId()).stream()
                .map(p -> new PaymentView(p.getId(), p.paidOn, p.amountReceived, p.tdsDeducted, p.mode, p.reference,
                        p.note, p.getCreatedBy()))
                .toList();
        List<ContactView> contacts = contactRepository.findByClientIdOrderByPrimaryContactDescNameAsc(i.clientId)
                .stream().filter(c -> c.getEmail() != null && !c.getEmail().isBlank())
                .map(c -> new ContactView(c.getName(), c.getEmail(), c.isPrimaryContact())).toList();
        return new InvoiceDetail(i.getId(), i.number, i.status, isOverdue(i, today), i.placementId, i.clientId,
                clientName, i.jobId, i.jobCode, i.jobTitle, i.candidateId, i.candidateName, i.invoiceDate, i.dueDate,
                i.joiningDate, i.description, i.sacCode, i.offeredCtc, i.feeType, i.feeValue, i.baseAmount, i.gstRate,
                i.cgstAmount, i.sgstAmount, i.igstAmount, i.totalAmount, i.tdsRate, i.tdsAmount, i.netReceivable,
                i.settledAmount, i.getBalance(), i.cancelReason, i.cancelledAt, i.emailStatus, i.emailSentAt,
                i.emailError, i.sentTo, payments, contacts, i.getVersion());
    }

    private static boolean isOverdue(Invoice i, LocalDate today) {
        return OPEN.contains(i.status) && i.dueDate.isBefore(today);
    }

    private static boolean sameState(String a, String b) {
        return !isBlank(a) && !isBlank(b) && a.trim().equalsIgnoreCase(b.trim());
    }

    private static String address(String line1, String line2, String city, String postal) {
        List<String> parts = new ArrayList<>();
        for (String part : new String[] {line1, line2, city, postal}) {
            if (!isBlank(part)) {
                parts.add(part.trim());
            }
        }
        return String.join(", ", parts);
    }

    private static String bank(CompanyProfile c) {
        List<String> lines = new ArrayList<>();
        add(lines, "Account name", c.getBankAccountName());
        add(lines, "Bank", c.getBankName());
        add(lines, "Account no.", c.getBankAccountNumber());
        add(lines, "IFSC", c.getBankIfsc());
        add(lines, "Branch", c.getBankBranch());
        return String.join("\n", lines);
    }

    private static void add(List<String> lines, String label, String value) {
        if (!isBlank(value)) {
            lines.add(label + ": " + value.trim());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blank(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
