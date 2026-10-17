package com.abchr.platform.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProfileDtos {

    static final String PHONE = "^[0-9+()\\- ]{7,20}$";
    static final String PINCODE = "^[0-9]{6}$";

    private ProfileDtos() {
    }

    public record Summary(
            UUID userId,
            String fullName,
            String email,
            boolean active,
            String employeeCode,
            String designation,
            String department,
            LocalDate joiningDate,
            String managerName,
            boolean bankComplete,
            int documentCount,
            int expiringDocuments) {
    }

    public record Sensitive(
            String bankAccountHolder,
            String bankName,
            String bankAccountNumber,
            String bankIfsc,
            String pan,
            String uan,
            boolean masked) {
    }

    public record View(
            UUID userId,
            String fullName,
            String email,
            boolean active,
            List<String> roles,
            String employeeCode,
            EmploymentType employmentType,
            String designation,
            String department,
            LocalDate joiningDate,
            UUID managerUserId,
            String managerName,
            String phone,
            LocalDate dateOfBirth,
            String addressLine,
            String city,
            String state,
            String pincode,
            String emergencyName,
            String emergencyPhone,
            String emergencyRelation,
            Sensitive sensitive,
            boolean canEdit,
            boolean canEditContact,
            boolean canEditSensitive,
            boolean canManageDocuments,
            Long version) {
    }

    public record ManagerOption(UUID id, String fullName) {
    }

    public record UpdateRequest(
            @Size(max = 30) String employeeCode,
            @NotNull EmploymentType employmentType,
            @Size(max = 100) String designation,
            @Size(max = 100) String department,
            LocalDate joiningDate,
            UUID managerUserId,
            @Pattern(regexp = PHONE) String phone,
            LocalDate dateOfBirth,
            @Size(max = 255) String addressLine,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Pattern(regexp = PINCODE) String pincode,
            @Size(max = 150) String emergencyName,
            @Pattern(regexp = PHONE) String emergencyPhone,
            @Size(max = 60) String emergencyRelation,
            Long version) {
    }

    public record ContactRequest(
            @Pattern(regexp = PHONE) String phone,
            @Size(max = 255) String addressLine,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Pattern(regexp = PINCODE) String pincode,
            @Size(max = 150) String emergencyName,
            @Pattern(regexp = PHONE) String emergencyPhone,
            @Size(max = 60) String emergencyRelation,
            Long version) {
    }

    public record SensitiveRequest(
            @Size(max = 150) String bankAccountHolder,
            @Size(max = 100) String bankName,
            @Pattern(regexp = "^[0-9]{9,18}$") String bankAccountNumber,
            @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$") String bankIfsc,
            @Pattern(regexp = "^[A-Za-z]{5}[0-9]{4}[A-Za-z]$") String pan,
            @Pattern(regexp = "^[0-9]{12}$") String uan,
            Long version) {
    }

    public record DocumentView(
            UUID id,
            DocumentType docType,
            String title,
            String fileName,
            long fileSize,
            LocalDate expiresOn,
            boolean expired,
            boolean expiringSoon,
            java.time.Instant uploadedAt,
            String reviewStatus,
            boolean uploadedBySelf,
            String reviewNote,
            String reviewedBy,
            java.time.Instant reviewedAt,
            boolean canDelete,
            boolean canReview) {
    }

    public record ReviewRequest(
            @NotBlank @Pattern(regexp = "VERIFIED|REJECTED") String decision, @Size(max = 300) String note) {
    }

    public record DocumentMeta(
            @NotNull DocumentType docType,
            @NotBlank @Size(max = 150) String title,
            LocalDate expiresOn) {
    }
}
