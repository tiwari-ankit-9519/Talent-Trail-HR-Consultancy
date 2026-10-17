package com.abchr.platform.replacement;

import com.abchr.platform.replacement.ReplacementDtos.CloseRequest;
import com.abchr.platform.replacement.ReplacementDtos.ExitRequest;
import com.abchr.platform.replacement.ReplacementDtos.LinkRequest;
import com.abchr.platform.replacement.ReplacementDtos.PlacementReplacement;
import com.abchr.platform.replacement.ReplacementDtos.ReplacementView;
import com.abchr.platform.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReplacementController {

    private static final String VIEW = "hasAnyAuthority('REPLACEMENTS_VIEW','REPLACEMENTS_MANAGE')";
    private static final String MANAGE = "hasAuthority('REPLACEMENTS_MANAGE')";

    private final ReplacementService service;

    public ReplacementController(ReplacementService service) {
        this.service = service;
    }

    @GetMapping("/replacements")
    @PreAuthorize(VIEW)
    public PageResponse<ReplacementView> search(
            @RequestParam(required = false) ReplacementStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(status, clientId, page, size);
    }

    @GetMapping("/placements/{placementId}/replacement")
    @PreAuthorize(VIEW)
    public PlacementReplacement forPlacement(@PathVariable UUID placementId) {
        return service.forPlacement(placementId);
    }

    @PostMapping("/placements/{placementId}/exit")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public ReplacementView recordExit(@PathVariable UUID placementId, @Valid @RequestBody ExitRequest request) {
        return service.recordExit(placementId, request);
    }

    @PostMapping("/replacements/{id}/link")
    @PreAuthorize(MANAGE)
    public ReplacementView link(@PathVariable UUID id, @Valid @RequestBody LinkRequest request) {
        return service.link(id, request);
    }

    @PostMapping("/replacements/{id}/close")
    @PreAuthorize(MANAGE)
    public ReplacementView close(@PathVariable UUID id, @Valid @RequestBody CloseRequest request) {
        return service.close(id, request);
    }
}
