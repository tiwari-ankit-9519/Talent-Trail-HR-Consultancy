package com.abchr.platform.imports;

import com.abchr.platform.imports.ImportDtos.ImportReport;
import com.abchr.platform.imports.ImportDtos.ImportRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/imports")
@PreAuthorize("isAuthenticated()")
public class ImportController {

    private final ImportService service;

    public ImportController(ImportService service) {
        this.service = service;
    }

    @PostMapping("/{type}/preview")
    public ImportReport preview(@PathVariable String type, @Valid @RequestBody ImportRequest request) {
        return service.preview(type, request);
    }

    @PostMapping("/{type}/commit")
    public ImportReport commit(@PathVariable String type, @Valid @RequestBody ImportRequest request) {
        return service.commit(type, request);
    }
}
