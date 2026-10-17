package com.abchr.platform.monitoring;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final SystemHealthService service;

    public SystemHealthController(SystemHealthService service) {
        this.service = service;
    }

    @GetMapping("/health")
    @PreAuthorize("hasAuthority('SYSTEM_VIEW')")
    public HealthReport health() {
        return service.run();
    }
}
