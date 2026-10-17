package com.abchr.platform.client;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "client")
public class Client extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "legal_name", length = 250)
    private String legalName;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 12)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 100)
    private String country = "India";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClientStatus status = ClientStatus.ACTIVE;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 30)
    private FeeType feeType = FeeType.MONTHS_OF_SALARY;

    @Column(name = "fee_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeValue = BigDecimal.ONE;

    @Column(name = "gst_applicable", nullable = false)
    private boolean gstApplicable = true;

    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate = new BigDecimal("18.00");

    @Column(name = "tds_applicable", nullable = false)
    private boolean tdsApplicable;

    @Column(name = "tds_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal tdsRate = BigDecimal.ZERO;

    @Column(name = "payment_terms_days", nullable = false)
    private int paymentTermsDays = 30;

    @Column(name = "replacement_period_days", nullable = false)
    private int replacementPeriodDays = 90;

    protected Client() {
    }

    public Client(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public BigDecimal getFeeValue() {
        return feeValue;
    }

    public boolean isGstApplicable() {
        return gstApplicable;
    }

    public BigDecimal getGstRate() {
        return gstRate;
    }

    public boolean isTdsApplicable() {
        return tdsApplicable;
    }

    public BigDecimal getTdsRate() {
        return tdsRate;
    }

    public int getPaymentTermsDays() {
        return paymentTermsDays;
    }

    public int getReplacementPeriodDays() {
        return replacementPeriodDays;
    }

    public void setTerms(
            FeeType feeType,
            BigDecimal feeValue,
            boolean gstApplicable,
            BigDecimal gstRate,
            boolean tdsApplicable,
            BigDecimal tdsRate,
            int paymentTermsDays,
            int replacementPeriodDays) {
        this.feeType = feeType;
        this.feeValue = feeValue;
        this.gstApplicable = gstApplicable;
        this.gstRate = gstRate;
        this.tdsApplicable = tdsApplicable;
        this.tdsRate = tdsRate;
        this.paymentTermsDays = paymentTermsDays;
        this.replacementPeriodDays = replacementPeriodDays;
    }
}
