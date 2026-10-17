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
@Table(name = "employee_profile")
public class EmployeeProfile extends AuditableEntity {

    @Id
    @Column(name = "user_id")
    UUID userId;

    @Column(name = "employee_code", length = 30)
    String employeeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Column(name = "designation", length = 100)
    String designation;

    @Column(name = "department", length = 100)
    String department;

    @Column(name = "joining_date")
    LocalDate joiningDate;

    @Column(name = "manager_user_id")
    UUID managerUserId;

    @Column(name = "phone", length = 20)
    String phone;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Column(name = "address_line", length = 255)
    String addressLine;

    @Column(name = "city", length = 100)
    String city;

    @Column(name = "state", length = 100)
    String state;

    @Column(name = "pincode", length = 10)
    String pincode;

    @Column(name = "emergency_name", length = 150)
    String emergencyName;

    @Column(name = "emergency_phone", length = 20)
    String emergencyPhone;

    @Column(name = "emergency_relation", length = 60)
    String emergencyRelation;

    @Column(name = "bank_account_holder", length = 150)
    String bankAccountHolder;

    @Column(name = "bank_name", length = 100)
    String bankName;

    @Column(name = "bank_ifsc", length = 11)
    String bankIfsc;

    @Column(name = "bank_account_enc")
    String bankAccountEnc;

    @Column(name = "pan_enc")
    String panEnc;

    @Column(name = "uan_enc")
    String uanEnc;

    protected EmployeeProfile() {
    }

    static EmployeeProfile forUser(UUID userId) {
        EmployeeProfile profile = new EmployeeProfile();
        profile.userId = userId;
        return profile;
    }

    public UUID getUserId() {
        return userId;
    }
}
