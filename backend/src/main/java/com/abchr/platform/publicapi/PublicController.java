package com.abchr.platform.publicapi;

import com.abchr.platform.job.JobDtos.PublicJob;
import com.abchr.platform.job.JobService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final JobService jobService;
    private final PublicApplyService applyService;

    public PublicController(JobService jobService, PublicApplyService applyService) {
        this.jobService = jobService;
        this.applyService = applyService;
    }

    @GetMapping("/jobs")
    public List<PublicJob> jobs() {
        return jobService.publicJobs();
    }

    @GetMapping("/jobs/{code}")
    public PublicJob job(@PathVariable String code) {
        return jobService.publicJob(code);
    }

    @PostMapping(value = "/jobs/{code}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> apply(
            @PathVariable String code,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String currentCompany,
            @RequestParam(required = false) String currentTitle,
            @RequestParam(required = false) String totalExperienceYears,
            @RequestParam(required = false) String currentCtc,
            @RequestParam(required = false) String expectedCtc,
            @RequestParam(required = false) String noticePeriodDays,
            @RequestParam(required = false) String currentLocation,
            @RequestParam(required = false) String skills,
            @RequestParam(defaultValue = "false") boolean consent,
            @RequestParam(required = false) String website,
            @RequestParam(value = "resume", required = false) MultipartFile resume,
            HttpServletRequest request) {
        applyService.apply(code, new PublicApplyService.ApplyForm(fullName, email, phone, currentCompany,
                currentTitle, totalExperienceYears, currentCtc, expectedCtc, noticePeriodDays, currentLocation,
                skills, consent, website, resume), request.getRemoteAddr());
        return Map.of("status", "RECEIVED");
    }
}
