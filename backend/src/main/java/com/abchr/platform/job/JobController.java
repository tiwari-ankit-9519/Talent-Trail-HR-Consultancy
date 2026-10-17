package com.abchr.platform.job;

import com.abchr.platform.job.JobDtos.JobDetail;
import com.abchr.platform.job.JobDtos.JobStatusRequest;
import com.abchr.platform.job.JobDtos.JobSummary;
import com.abchr.platform.job.JobDtos.SaveJobRequest;
import com.abchr.platform.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private static final String VIEW = "hasAnyAuthority('JOBS_VIEW','JOBS_MANAGE')";
    private static final String MANAGE = "hasAuthority('JOBS_MANAGE')";

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @PreAuthorize(VIEW)
    public PageResponse<JobSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobService.search(q, status, clientId, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW)
    public JobDetail get(@PathVariable UUID id) {
        return jobService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public JobDetail create(@Valid @RequestBody SaveJobRequest request) {
        return jobService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(MANAGE)
    public JobDetail update(@PathVariable UUID id, @Valid @RequestBody SaveJobRequest request) {
        return jobService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(MANAGE)
    public JobDetail changeStatus(@PathVariable UUID id, @Valid @RequestBody JobStatusRequest request) {
        return jobService.changeStatus(id, request);
    }
}
