package com.abchr.platform.profile;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.profile.ProfileDtos.DocumentMeta;
import com.abchr.platform.notification.NotificationService;
import com.abchr.platform.profile.ProfileDtos.DocumentView;
import com.abchr.platform.profile.ProfileDtos.ReviewRequest;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileDocumentService {

    private static final int EXPIRY_WARNING_DAYS = 30;

    public record DownloadFile(Path path, String fileName, String contentType) {
    }

    private final EmployeeDocumentRepository documents;
    private final EmployeeDocumentStorage storage;
    private final UserRepository users;
    private final AuditService auditService;
    private final NotificationService notifications;

    public ProfileDocumentService(
            EmployeeDocumentRepository documents,
            EmployeeDocumentStorage storage,
            UserRepository users,
            AuditService auditService,
            NotificationService notifications) {
        this.documents = documents;
        this.storage = storage;
        this.users = users;
        this.auditService = auditService;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<DocumentView> list(UUID userId) {
        requireView(userId);
        return documents.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toView).toList();
    }

    @Transactional
    public DocumentView addOwn(DocumentMeta meta, MultipartFile file) {
        return add(Actor.current().id(), meta, file, true);
    }

    @Transactional
    public DocumentView add(UUID userId, DocumentMeta meta, MultipartFile file) {
        return add(userId, meta, file, false);
    }

    private DocumentView add(UUID userId, DocumentMeta meta, MultipartFile file, boolean own) {
        boolean needsReview = own && !Permissions.hasAny("PROFILES_MANAGE");
        AppUser user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.USER_NOT_FOUND, "User not found"));
        EmployeeDocumentStorage.Stored stored = storage.store(userId, file);
        EmployeeDocument document = EmployeeDocument.create(userId);
        document.docType = meta.docType();
        document.title = meta.title().trim();
        document.filePath = stored.path();
        document.fileName = stored.fileName();
        document.contentType = stored.contentType();
        document.fileSize = stored.size();
        document.expiresOn = meta.expiresOn();
        document.uploadedBySelf = own;
        document.reviewStatus = needsReview ? "PENDING" : "VERIFIED";
        try {
            documents.saveAndFlush(document);
        } catch (RuntimeException ex) {
            storage.delete(stored.path());
            throw ex;
        }
        auditService.record(AuditActions.PROFILE_DOCUMENT_ADDED, AuditActions.ENTITY_PROFILE_DOCUMENT,
                document.getId().toString(), (own ? "Uploaded " : "Added ") + document.title + " for " + user.getFullName(), null);
        if (needsReview) {
            notifications.notifyUsers(notifications.usersWithAnyPermission("PROFILES_MANAGE"), "PROFILE",
                    "Document to verify", user.getFullName() + " uploaded " + document.title, "/people/" + userId);
        }
        return toView(document);
    }

    @Transactional
    public DownloadFile download(UUID documentId) {
        EmployeeDocument document = find(documentId);
        requireView(document.userId);
        auditService.record(AuditActions.PROFILE_DOCUMENT_DOWNLOADED, AuditActions.ENTITY_PROFILE_DOCUMENT,
                documentId.toString(), "Downloaded " + document.title, null);
        return new DownloadFile(storage.resolve(document.filePath), document.fileName, document.contentType);
    }

    @Transactional
    public DocumentView review(UUID documentId, ReviewRequest request) {
        EmployeeDocument document = find(documentId);
        if (!"PENDING".equals(document.reviewStatus)) {
            throw ApiException.rule(ErrorCodes.PROFILE_DOCUMENT_REVIEW_INVALID, "Only a pending document can be reviewed");
        }
        String note = request.note() == null ? "" : request.note().trim();
        if ("REJECTED".equals(request.decision()) && note.isEmpty()) {
            throw ApiException.rule(ErrorCodes.PROFILE_DOCUMENT_REVIEW_INVALID, "Please give a reason for rejecting");
        }
        document.reviewStatus = request.decision();
        document.reviewNote = note.isEmpty() ? null : note;
        document.reviewedBy = Actor.current().email();
        document.reviewedAt = java.time.Instant.now();
        documents.saveAndFlush(document);
        auditService.record(AuditActions.PROFILE_DOCUMENT_REVIEWED, AuditActions.ENTITY_PROFILE_DOCUMENT,
                documentId.toString(), request.decision() + " " + document.title, null);
        boolean ok = "VERIFIED".equals(request.decision());
        notifications.notifyUsers(List.of(document.userId), "PROFILE",
                ok ? "Document verified" : "Document rejected",
                document.title + (ok || note.isEmpty() ? "" : ": " + note), "/profile");
        return toView(document);
    }

    @Transactional
    public void delete(UUID documentId) {
        EmployeeDocument document = find(documentId);
        if (!canDelete(document)) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "You do not have permission to do this");
        }
        documents.delete(document);
        documents.flush();
        storage.delete(document.filePath);
        auditService.record(AuditActions.PROFILE_DOCUMENT_DELETED, AuditActions.ENTITY_PROFILE_DOCUMENT,
                documentId.toString(), "Deleted " + document.title, null);
    }

    private EmployeeDocument find(UUID documentId) {
        return documents.findById(documentId).orElseThrow(
                () -> ApiException.notFound(ErrorCodes.PROFILE_DOCUMENT_NOT_FOUND, "Document not found"));
    }

    private void requireView(UUID userId) {
        boolean self = userId.equals(Actor.current().id()) && Permissions.hasAny("PROFILE_SELF");
        if (!self && !Permissions.hasAny("PROFILES_VIEW", "PROFILES_MANAGE")) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "You do not have permission to do this");
        }
    }

    private boolean canDelete(EmployeeDocument d) {
        if (Permissions.hasAny("PROFILES_MANAGE")) {
            return true;
        }
        return d.uploadedBySelf && d.userId.equals(Actor.current().id()) && Permissions.hasAny("PROFILE_SELF")
                && !"VERIFIED".equals(d.reviewStatus);
    }

    private DocumentView toView(EmployeeDocument d) {
        LocalDate today = LocalDate.now();
        boolean expired = d.expiresOn != null && d.expiresOn.isBefore(today);
        boolean soon = d.expiresOn != null && !expired && !d.expiresOn.isAfter(today.plusDays(EXPIRY_WARNING_DAYS));
        return new DocumentView(d.getId(), d.docType, d.title, d.fileName, d.fileSize, d.expiresOn, expired, soon,
                d.getCreatedAt(), d.reviewStatus, d.uploadedBySelf, d.reviewNote, d.reviewedBy, d.reviewedAt,
                canDelete(d), Permissions.hasAny("PROFILES_MANAGE") && "PENDING".equals(d.reviewStatus));
    }
}
