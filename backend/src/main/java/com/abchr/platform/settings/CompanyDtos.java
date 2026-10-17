package com.abchr.platform.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CompanyDtos {

    public static final String GSTIN_PATTERN = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";
    public static final String PAN_PATTERN = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$";
    public static final String IFSC_PATTERN = "^$|^[A-Z]{4}0[A-Z0-9]{6}$";

    private CompanyDtos() {
    }

    public record CompanyRequest(
            @NotBlank @Size(max = 250) String legalName,
            @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 12) String postalCode,
            @Size(max = 15) @Pattern(regexp = GSTIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE) String gstin,
            @Size(max = 10) @Pattern(regexp = PAN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE) String pan,
            @Size(max = 255) String email,
            @Size(max = 30) String phone,
            @NotBlank @Size(max = 10) String sacCode,
            @NotBlank @Size(max = 10) @Pattern(regexp = "^[A-Za-z0-9]+$") String invoicePrefix,
            @Size(max = 200) String bankAccountName,
            @Size(max = 200) String bankName,
            @Size(max = 40) String bankAccountNumber,
            @Size(max = 15) @Pattern(regexp = IFSC_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE) String bankIfsc,
            @Size(max = 200) String bankBranch,
            @Size(max = 2000) String invoiceFooter,
            Long version) {
    }

    public record CompanyResponse(
            String legalName,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String gstin,
            String pan,
            String email,
            String phone,
            String sacCode,
            String invoicePrefix,
            String bankAccountName,
            String bankName,
            String bankAccountNumber,
            String bankIfsc,
            String bankBranch,
            String invoiceFooter,
            Long version) {
    }
}
