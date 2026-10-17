package com.abchr.platform.profile;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employee_document")
public class EmployeeDocument extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 30)
    DocumentType docType;

    @Column(name = "title", nullable = false, length = 150)
    String title;

    @Column(name = "file_path", nullable = false, length = 400)
    String filePath;

    @Column(name = "file_name", nullable = false, length = 200)
    String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    String contentType;

    @Column(name = "file_size", nullable = false)
    long fileSize;

    @Column(name = "expires_on")
    LocalDate expiresOn;

    @Column(name = "review_status", nullable = false, length = 10)
    String reviewStatus = "VERIFIED";

    @Column(name = "uploaded_by_self", nullable = false)
    boolean uploadedBySelf;

    @Column(name = "reviewed_by", length = 255)
    String reviewedBy;

    @Column(name = "reviewed_at")
    java.time.Instant reviewedAt;

    @Column(name = "review_note", length = 300)
    String reviewNote;

    protected EmployeeDocument() {
    }

    static EmployeeDocument create(UUID userId) {
        EmployeeDocument document = new EmployeeDocument();
        document.userId = userId;
        return document;
    }

    public UUID getId() {
        return id;
    }
}
