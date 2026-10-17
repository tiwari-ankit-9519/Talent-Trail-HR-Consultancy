package com.abchr.platform.candidate;

import com.abchr.platform.candidate.CandidateDtos.CandidateDetail;
import com.abchr.platform.candidate.CandidateDtos.CandidateStatusRequest;
import com.abchr.platform.candidate.CandidateDtos.CandidateSummary;
import com.abchr.platform.candidate.CandidateDtos.ConsentRequest;
import com.abchr.platform.candidate.CandidateDtos.LinkJobRequest;
import com.abchr.platform.candidate.CandidateDtos.SaveCandidateRequest;
import com.abchr.platform.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class CandidateController {

    private static final String VIEW = "hasAnyAuthority('CANDIDATES_VIEW','CANDIDATES_MANAGE')";
    private static final String MANAGE = "hasAuthority('CANDIDATES_MANAGE')";
    private static final String JOB_VIEW = "hasAnyAuthority('JOBS_VIEW','JOBS_MANAGE')"
            + " and hasAnyAuthority('CANDIDATES_VIEW','CANDIDATES_MANAGE')";

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping("/candidates")
    @PreAuthorize(VIEW)
    public PageResponse<CandidateSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(required = false) ConsentStatus consent,
            @RequestParam(required = false) CandidateSource source,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return candidateService.search(q, status, consent, source, jobId, page, size);
    }

    @GetMapping("/candidates/{id}")
    @PreAuthorize(VIEW)
    public CandidateDetail get(@PathVariable UUID id) {
        return candidateService.get(id);
    }

    @PostMapping("/candidates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public CandidateDetail create(@Valid @RequestBody SaveCandidateRequest request) {
        return candidateService.create(request);
    }

    @PutMapping("/candidates/{id}")
    @PreAuthorize(MANAGE)
    public CandidateDetail update(@PathVariable UUID id, @Valid @RequestBody SaveCandidateRequest request) {
        return candidateService.update(id, request);
    }

    @PatchMapping("/candidates/{id}/status")
    @PreAuthorize(MANAGE)
    public CandidateDetail changeStatus(@PathVariable UUID id, @Valid @RequestBody CandidateStatusRequest request) {
        return candidateService.changeStatus(id, request);
    }

    @PostMapping("/candidates/{id}/consent")
    @PreAuthorize(MANAGE)
    public CandidateDetail recordConsent(@PathVariable UUID id, @Valid @RequestBody ConsentRequest request) {
        return candidateService.recordConsent(id, request);
    }

    @PostMapping(value = "/candidates/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(MANAGE)
    public CandidateDetail uploadResume(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return candidateService.uploadResume(id, file);
    }

    @GetMapping("/candidates/{id}/resume")
    @PreAuthorize(VIEW)
    public ResponseEntity<Resource> downloadResume(@PathVariable UUID id) throws IOException {
        CandidateService.ResumeFile file = candidateService.downloadResume(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(new FileSystemResource(file.path()));
    }

    @PostMapping("/candidates/{id}/applications")
    @PreAuthorize(MANAGE)
    public CandidateDetail link(@PathVariable UUID id, @Valid @RequestBody LinkJobRequest request) {
        return candidateService.linkToJob(id, request.jobId());
    }

    @DeleteMapping("/candidates/{id}/applications/{applicationId}")
    @PreAuthorize(MANAGE)
    public CandidateDetail unlink(@PathVariable UUID id, @PathVariable UUID applicationId) {
        return candidateService.unlinkFromJob(id, applicationId);
    }

    @GetMapping("/jobs/{jobId}/candidates")
    @PreAuthorize(JOB_VIEW)
    public List<CandidateSummary> candidatesForJob(@PathVariable UUID jobId) {
        return candidateService.candidatesForJob(jobId);
    }
}
