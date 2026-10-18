package com.abchr.platform.placement;

import com.abchr.platform.placement.PlacementDtos.AcceptRequest;
import com.abchr.platform.placement.PlacementDtos.JoinedRequest;
import com.abchr.platform.placement.PlacementDtos.OfferRequest;
import com.abchr.platform.placement.PlacementDtos.PlacementDetail;
import com.abchr.platform.placement.PlacementDtos.PlacementSummary;
import com.abchr.platform.placement.PlacementDtos.ReasonRequest;
import com.abchr.platform.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
public class PlacementController {

    private static final String VIEW = "hasAnyAuthority('PLACEMENTS_VIEW','PLACEMENTS_MANAGE')";
    private static final String MANAGE = "hasAuthority('PLACEMENTS_MANAGE')";

    private final PlacementService placementService;
    private final PlacementMailService mailService;

    public PlacementController(PlacementService placementService, PlacementMailService mailService) {
        this.placementService = placementService;
        this.mailService = mailService;
    }

    @GetMapping("/placements")
    @PreAuthorize(VIEW)
    public PageResponse<PlacementSummary> search(
            @RequestParam(required = false) PlacementStatus status,
            @RequestParam(required = false) UUID submissionId,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) ExitFilter exit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return placementService.search(status, submissionId, jobId, clientId, candidateId, exit, page, size);
    }

    @GetMapping("/placements/{id}")
    @PreAuthorize(VIEW)
    public PlacementDetail get(@PathVariable UUID id) {
        return placementService.get(id);
    }

    @PostMapping("/submissions/{submissionId}/placement")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public PlacementDetail createOffer(@PathVariable UUID submissionId, @Valid @RequestBody OfferRequest request) {
        PlacementDetail created = placementService.createOffer(submissionId, request);
        if (request.sendCandidateEmail()) {
            mailService.sendOfferEmail(created.id());
            return placementService.get(created.id());
        }
        return created;
    }

    @PostMapping("/placements/{id}/offer-email")
    @PreAuthorize(MANAGE)
    public PlacementDetail sendOfferEmail(@PathVariable UUID id) {
        mailService.sendOfferEmail(id);
        return placementService.get(id);
    }

    @PutMapping("/placements/{id}/offer")
    @PreAuthorize(MANAGE)
    public PlacementDetail updateOffer(@PathVariable UUID id, @Valid @RequestBody OfferRequest request) {
        return placementService.updateOffer(id, request);
    }

    @PostMapping("/placements/{id}/accept")
    @PreAuthorize(MANAGE)
    public PlacementDetail accept(@PathVariable UUID id, @Valid @RequestBody AcceptRequest request) {
        return placementService.accept(id, request);
    }

    @PostMapping("/placements/{id}/decline")
    @PreAuthorize(MANAGE)
    public PlacementDetail decline(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return placementService.decline(id, request);
    }

    @PostMapping("/placements/{id}/joined")
    @PreAuthorize(MANAGE)
    public PlacementDetail joined(@PathVariable UUID id, @Valid @RequestBody JoinedRequest request) {
        return placementService.markJoined(id, request);
    }

    @PostMapping("/placements/{id}/not-joined")
    @PreAuthorize(MANAGE)
    public PlacementDetail notJoined(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return placementService.markNotJoined(id, request);
    }

    @PostMapping(value = "/placements/{id}/joining-email", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(MANAGE)
    public PlacementDetail uploadJoiningEmail(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return placementService.uploadJoiningEmail(id, file);
    }

    @GetMapping("/placements/{id}/joining-email")
    @PreAuthorize(VIEW)
    public ResponseEntity<Resource> downloadJoiningEmail(@PathVariable UUID id) {
        PlacementService.JoiningEmailFile file = placementService.downloadJoiningEmail(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(new FileSystemResource(file.path()));
    }
}
