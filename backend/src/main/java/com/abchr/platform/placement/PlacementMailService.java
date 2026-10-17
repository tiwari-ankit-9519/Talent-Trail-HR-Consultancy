package com.abchr.platform.placement;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.candidate.Candidate;
import com.abchr.platform.candidate.CandidateRepository;
import com.abchr.platform.client.Client;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.job.Job;
import com.abchr.platform.job.JobRepository;
import com.abchr.platform.reminder.EmailTemplateService;
import com.abchr.platform.reminder.ReminderDtos.Rendered;
import com.abchr.platform.reminder.TemplateCatalog;
import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.mail.EmailHandler;
import com.abchr.platform.shared.mail.EmailQueue;
import com.abchr.platform.shared.mail.MailService;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PlacementMailService implements EmailHandler {

    private static final Logger LOG = LogManager.getLogger(PlacementMailService.class);
    private static final String EMAIL_KIND = "OFFER";

    private final PlacementRepository placementRepository;
    private final JobRepository jobRepository;
    private final ClientRepository clientRepository;
    private final CandidateRepository candidateRepository;
    private final MailService mailService;
    private final EmailQueue emailQueue;
    private final AuditService auditService;
    private final EmailTemplateService templateService;
    private final TransactionTemplate tx;
    private final DateTimeFormatter day;

    public PlacementMailService(
            PlacementRepository placementRepository,
            JobRepository jobRepository,
            ClientRepository clientRepository,
            CandidateRepository candidateRepository,
            MailService mailService,
            EmailQueue emailQueue,
            AuditService auditService,
            EmailTemplateService templateService,
            PlatformTransactionManager transactionManager) {
        this.placementRepository = placementRepository;
        this.jobRepository = jobRepository;
        this.clientRepository = clientRepository;
        this.candidateRepository = candidateRepository;
        this.mailService = mailService;
        this.emailQueue = emailQueue;
        this.auditService = auditService;
        this.templateService = templateService;
        this.tx = new TransactionTemplate(transactionManager);
        this.day = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    }

    public void sendOfferEmail(UUID placementId) {
        String actor = Actor.current().email();
        tx.executeWithoutResult(status -> {
            Placement placement = placementRepository.findById(placementId)
                    .orElseThrow(() -> ApiException.notFound(ErrorCodes.PLACEMENT_NOT_FOUND, "Offer not found"));
            if (!placement.isOpen()) {
                throw ApiException.rule(ErrorCodes.OFFER_NOT_OPEN, "Only an open offer can be emailed");
            }
            placement.recordOfferEmail(EmailStatus.QUEUED, null);
            placementRepository.saveAndFlush(placement);
            emailQueue.enqueue(EMAIL_KIND, placementId, actor, List.of());
        });
    }

    @Override
    public String kind() {
        return EMAIL_KIND;
    }

    @Override
    public void handle(UUID id, List<String> args) {
        deliverOffer(id);
    }

    @Override
    public void fail(UUID id, List<String> args, String reason) {
        tx.executeWithoutResult(status -> {
            Placement fresh = placementRepository.findById(id).orElseThrow();
            fresh.recordOfferEmail(EmailStatus.FAILED, reason.length() > 480 ? reason.substring(0, 480) : reason);
            placementRepository.saveAndFlush(fresh);
        });
    }

    private void deliverOffer(UUID placementId) {
        Placement placement = placementRepository.findById(placementId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.PLACEMENT_NOT_FOUND, "Offer not found"));
        Job job = jobRepository.findById(placement.getJobId()).orElseThrow();
        Client client = clientRepository.findById(placement.getClientId()).orElseThrow();
        Candidate candidate = candidateRepository.findById(placement.getCandidateId()).orElseThrow();
        Rendered rendered = templateService.render(TemplateCatalog.OFFER_EMAIL, offerValues(placement, job, client, candidate));
        MailService.Result result = mailService.send(List.of(candidate.getEmail()), rendered.subject(),
                rendered.body(), null, Actor.current().email());
        tx.executeWithoutResult(status -> {
            Placement fresh = placementRepository.findById(placementId).orElseThrow();
            fresh.recordOfferEmail(result.status(), result.error());
            placementRepository.saveAndFlush(fresh);
            LOG.info("Offer email for placement {} to candidate {}: {}", placementId, candidate.getId(),
                    result.status());
            auditService.record(AuditActions.OFFER_EMAIL, AuditActions.ENTITY_PLACEMENT, placementId.toString(),
                    "Congratulation email to " + candidate.getFullName() + ": " + result.status()
                            + (result.error() == null ? "" : " (" + result.error() + ")"), null);
        });
    }

    private Map<String, String> offerValues(Placement placement, Job job, Client client, Candidate candidate) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("candidate", candidate.getFullName());
        values.put("client", client.getName());
        values.put("jobTitle", job.getTitle());
        values.put("ctc", "INR " + NumberFormat.getNumberInstance(Locale.of("en", "IN")).format(placement.getOfferedCtc()));
        values.put("joiningDate", day.format(placement.getExpectedJoiningDate()));
        return values;
    }
}
