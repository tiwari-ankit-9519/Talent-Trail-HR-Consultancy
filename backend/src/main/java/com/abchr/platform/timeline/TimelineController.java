package com.abchr.platform.timeline;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/timeline")
public class TimelineController {

    private final TimelineService service;

    public TimelineController(TimelineService service) {
        this.service = service;
    }

    @GetMapping("/{subject}/{id}")
    public List<TimelineEvent> get(@PathVariable TimelineService.Subject subject, @PathVariable UUID id) {
        return service.forSubject(subject, id);
    }
}
