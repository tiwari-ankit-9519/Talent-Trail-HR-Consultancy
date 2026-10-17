package com.abchr.platform.publicapi;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditOutcome;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.candidate.Candidate;
import com.abchr.platform.candidate.CandidateConsentEvent;
import com.abchr.platform.candidate.CandidateConsentEventRepository;
import com.abchr.platform.candidate.CandidateRepository;
import com.abchr.platform.candidate.CandidateSource;
import com.abchr.platform.candidate.CandidateStatus;
import com.abchr.platform.candidate.ConsentMethod;
import com.abchr.platform.candidate.ConsentStatus;
import com.abchr.platform.candidate.JobApplication;
import com.abchr.platform.candidate.JobApplicationRepository;
import com.abchr.platform.candidate.ResumeStorage;
import com.abchr.platform.job.Job;
import com.abchr.platform.job.JobService;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PublicApplyService {

    private static final Logger LOG = LogManager.getLogger(PublicApplyService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String ACTOR = "public-apply";

    public record ApplyForm(
            String fullName,
            String email,
            String phone,
            String currentCompany,
            String currentTitle,
            String totalExperienceYears,
            String currentCtc,
            String expectedCtc,
            String noticePeriodDays,
            String currentLocation,
            String skills,
            boolean consent,
            String honeypot,
            MultipartFile resume) {
    }

    private final JobService jobService;
    private final CandidateRepository candidateRepository;
    private final CandidateConsentEventRepository consentRepository;
    private final JobApplicationRepository applicationRepository;
    private final ResumeStorage resumeStorage;
    private final AuditService auditService;
    private final ApplyRateLimiter rateLimiter;

    public PublicApplyService(
            JobService jobService,
            CandidateRepository candidateRepository,
            CandidateConsentEventRepository consentRepository,
            JobApplicationRepository applicationRepository,
            ResumeStorage resumeStorage,
            AuditService auditService,
            ApplyRateLimiter rateLimiter) {
        this.jobService = jobService;
        this.candidateRepository = candidateRepository;
        this.consentRepository = consentRepository;
        this.applicationRepository = applicationRepository;
        this.resumeStorage = resumeStorage;
        this.auditService = auditService;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public void apply(String jobCode, ApplyForm form, String clientIp) {
        rateLimiter.check(clientIp == null ? "unknown" : clientIp);
        if (form.honeypot() != null && !form.honeypot().isBlank()) {
            LOG.warn("Honeypot field filled on application for {}", jobCode);
            return;
        }
        LOG.info("Public application received for {} from ip={} resumeAttached={}", jobCode, clientIp,
                form.resume() != null && !form.resume().isEmpty());
        Job job = jobService.requireAcceptingByCode(jobCode);
        String name = required(form.fullName(), "Full name", 150);
        String email = required(form.email(), "Email", 255).toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw ApiException.rule(ErrorCodes.VALIDATION_FAILED, "Please enter a valid email address");
        }
        if (!form.consent()) {
            throw ApiException.rule(ErrorCodes.VALIDATION_FAILED,
                    "You must agree to share your profile with our client companies to apply");
        }
        BigDecimal experience = decimal(form.totalExperienceYears(), "Experience");
        BigDecimal currentCtc = decimal(form.currentCtc(), "Current CTC");
        BigDecimal expectedCtc = decimal(form.expectedCtc(), "Expected CTC");
        Integer notice = integer(form.noticePeriodDays(), "Notice period");

        Candidate existing = candidateRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null && existing.getStatus() == CandidateStatus.BLOCKED) {
            LOG.warn("Ignored application from a blocked candidate for {}", jobCode);
            return;
        }
        Candidate candidate;
        boolean created = existing == null;
        if (created) {
            candidate = new Candidate(name, email, CandidateSource.APPLY_LINK);
            candidate.setPhone(clean(form.phone(), 30));
            candidate.setCurrentCompany(clean(form.currentCompany(), 200));
            candidate.setCurrentTitle(clean(form.currentTitle(), 200));
            candidate.setTotalExperienceYears(experience);
            candidate.setCurrentCtc(currentCtc);
            candidate.setExpectedCtc(expectedCtc);
            candidate.setNoticePeriodDays(notice);
            candidate.setCurrentLocation(clean(form.currentLocation(), 150));
            candidate.setSkills(clean(form.skills(), 2000));
            ResumeStorage.Stored stored = resumeStorage.store(candidate.getId(), form.resume());
            candidate.setResume(stored.path(), stored.fileName(), stored.contentType(), stored.size());
            candidateRepository.save(candidate);
        } else {
            candidate = existing;
            if (!candidate.hasResume() && form.resume() != null && !form.resume().isEmpty()) {
                ResumeStorage.Stored stored = resumeStorage.store(candidate.getId(), form.resume());
                candidate.setResume(stored.path(), stored.fileName(), stored.contentType(), stored.size());
            }
        }
        if (created || candidate.getConsentStatus() == ConsentStatus.NOT_ASKED) {
            Instant now = Instant.now();
            candidate.recordConsent(ConsentStatus.GRANTED, ConsentMethod.APPLY_FORM, now);
            consentRepository.save(new CandidateConsentEvent(candidate.getId(), ConsentStatus.GRANTED,
                    ConsentMethod.APPLY_FORM, "Agreed on the apply form for " + job.getCode(), ACTOR, now));
        }
        candidateRepository.save(candidate);
        boolean linked = !applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId());
        if (linked) {
            applicationRepository.save(new JobApplication(job.getId(), candidate.getId(), CandidateSource.APPLY_LINK));
        }
        LOG.info("Public application stored: job={} candidate={} newCandidate={} newLink={}", job.getCode(),
                candidate.getId(), created, linked);
        auditService.recordAs(null, ACTOR, AuditActions.PUBLIC_APPLICATION, AuditActions.ENTITY_CANDIDATE,
                candidate.getId().toString(),
                (created ? "New candidate " : "Existing candidate ") + candidate.getFullName() + " applied to "
                        + job.getCode() + (linked ? "" : " again"),
                null, AuditOutcome.SUCCESS);
    }

    private static String required(String value, String label, int max) {
        String cleaned = clean(value, max);
        if (cleaned == null) {
            throw ApiException.rule(ErrorCodes.VALIDATION_FAILED, label + " is required");
        }
        return cleaned;
    }

    private static String clean(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private static BigDecimal decimal(String value, String label) {
        String cleaned = clean(value, 20);
        if (cleaned == null) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(cleaned);
            if (parsed.signum() < 0 || parsed.compareTo(new BigDecimal("1000000000")) > 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw ApiException.rule(ErrorCodes.VALIDATION_FAILED, label + " is not a valid number");
        }
    }

    private static Integer integer(String value, String label) {
        String cleaned = clean(value, 5);
        if (cleaned == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(cleaned);
            if (parsed < 0 || parsed > 365) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw ApiException.rule(ErrorCodes.VALIDATION_FAILED, label + " must be between 0 and 365 days");
        }
    }
}
