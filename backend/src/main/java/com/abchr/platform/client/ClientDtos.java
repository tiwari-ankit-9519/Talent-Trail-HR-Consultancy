package com.abchr.platform.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ClientDtos {

    public static final String GSTIN_PATTERN = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";
    public static final String PAN_PATTERN = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$";

    private ClientDtos() {
    }

    public record ProfileRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 250) String legalName,
            @Size(max = 100) String industry,
            @Size(max = 255) String website,
            @Size(max = 15) @Pattern(regexp = GSTIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE) String gstin,
            @Size(max = 10) @Pattern(regexp = PAN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE) String pan,
            @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 12) String postalCode,
            @Size(max = 100) String country,
            String notes,
            Long version) {
    }

    public record StatusRequest(@NotNull ClientStatus status) {
    }

    public record TermsRequest(
            @NotNull FeeType feeType,
            @NotNull BigDecimal feeValue,
            boolean gstApplicable,
            @NotNull BigDecimal gstRate,
            boolean tdsApplicable,
            @NotNull BigDecimal tdsRate,
            @NotNull Integer paymentTermsDays,
            @NotNull Integer replacementPeriodDays,
            Long version) {
    }

    public record ContactRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 30) String phone,
            @Size(max = 100) String designation,
            boolean primaryContact) {
    }

    public record ClientSummary(
            UUID id,
            String name,
            String industry,
            String city,
            String state,
            ClientStatus status,
            Instant updatedAt) {
    }

    public record ContactResponse(
            UUID id,
            String name,
            String email,
            String phone,
            String designation,
            boolean primaryContact) {
    }

    public record TermsResponse(
            FeeType feeType,
            BigDecimal feeValue,
            boolean gstApplicable,
            BigDecimal gstRate,
            boolean tdsApplicable,
            BigDecimal tdsRate,
            int paymentTermsDays,
            int replacementPeriodDays) {
    }

    public record ClientDetail(
            UUID id,
            String name,
            String legalName,
            String industry,
            String website,
            String gstin,
            String pan,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            ClientStatus status,
            String notes,
            TermsResponse terms,
            List<ContactResponse> contacts,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
    }
}
