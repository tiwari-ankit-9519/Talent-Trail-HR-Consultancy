package com.abchr.platform.profile;

import com.abchr.platform.profile.ProfileDtos.ContactRequest;
import com.abchr.platform.profile.ProfileDtos.DocumentMeta;
import com.abchr.platform.profile.ProfileDtos.DocumentView;
import com.abchr.platform.profile.ProfileDtos.ManagerOption;
import com.abchr.platform.profile.ProfileDtos.ReviewRequest;
import com.abchr.platform.profile.ProfileDtos.SensitiveRequest;
import com.abchr.platform.profile.ProfileDtos.Summary;
import com.abchr.platform.profile.ProfileDtos.UpdateRequest;
import com.abchr.platform.profile.ProfileDtos.View;
import com.abchr.platform.shared.security.Actor;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private static final String VIEW = "hasAnyAuthority('PROFILES_VIEW','PROFILES_MANAGE')";
    private static final String MANAGE = "hasAuthority('PROFILES_MANAGE')";
    private static final String SENSITIVE = "hasAuthority('PROFILES_SENSITIVE')";
    private static final String SELF = "hasAuthority('PROFILE_SELF')";

    private final ProfileService profileService;
    private final ProfileDocumentService documentService;

    public ProfileController(ProfileService profileService, ProfileDocumentService documentService) {
        this.profileService = profileService;
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize(VIEW)
    public List<Summary> list(@RequestParam(required = false) String q) {
        return profileService.list(q);
    }

    @GetMapping("/managers")
    @PreAuthorize(MANAGE)
    public List<ManagerOption> managers() {
        return profileService.managerOptions();
    }

    @GetMapping("/me")
    @PreAuthorize(SELF)
    public View mine() {
        return profileService.get(Actor.current().id());
    }

    @PutMapping("/me/contact")
    @PreAuthorize(SELF)
    public View updateContact(@Valid @RequestBody ContactRequest request) {
        return profileService.updateContact(request);
    }

    @GetMapping("/me/documents")
    @PreAuthorize(SELF)
    public List<DocumentView> myDocuments() {
        return documentService.list(Actor.current().id());
    }

    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SELF)
    public DocumentView addOwnDocument(
            @RequestParam("docType") DocumentType docType,
            @RequestParam("title") String title,
            @RequestParam(value = "expiresOn", required = false) LocalDate expiresOn,
            @RequestParam("file") MultipartFile file) {
        return documentService.addOwn(new DocumentMeta(docType, title, expiresOn), file);
    }

    @PostMapping("/documents/{documentId}/review")
    @PreAuthorize(MANAGE)
    public DocumentView reviewDocument(@PathVariable UUID documentId, @Valid @RequestBody ReviewRequest request) {
        return documentService.review(documentId, request);
    }

    @GetMapping("/{userId}")
    public View get(@PathVariable UUID userId) {
        return profileService.get(userId);
    }

    @PutMapping("/{userId}")
    @PreAuthorize(MANAGE)
    public View update(@PathVariable UUID userId, @Valid @RequestBody UpdateRequest request) {
        return profileService.update(userId, request);
    }

    @PutMapping("/{userId}/sensitive")
    @PreAuthorize(SENSITIVE)
    public View updateSensitive(@PathVariable UUID userId, @Valid @RequestBody SensitiveRequest request) {
        return profileService.updateSensitive(userId, request);
    }

    @GetMapping("/{userId}/documents")
    public List<DocumentView> documents(@PathVariable UUID userId) {
        return documentService.list(userId);
    }

    @PostMapping(value = "/{userId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public DocumentView addDocument(
            @PathVariable UUID userId,
            @RequestParam("docType") DocumentType docType,
            @RequestParam("title") String title,
            @RequestParam(value = "expiresOn", required = false) LocalDate expiresOn,
            @RequestParam("file") MultipartFile file) {
        return documentService.add(userId, new DocumentMeta(docType, title, expiresOn), file);
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<Resource> download(@PathVariable UUID documentId) {
        ProfileDocumentService.DownloadFile file = documentService.download(documentId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(new FileSystemResource(file.path()));
    }

    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('PROFILES_MANAGE','PROFILE_SELF')")
    public void deleteDocument(@PathVariable UUID documentId) {
        documentService.delete(documentId);
    }
}
