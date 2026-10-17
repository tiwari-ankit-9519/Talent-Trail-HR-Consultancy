package com.abchr.platform.role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "permission")
public class Permission {

    @Id
    @Column(name = "code", length = 60)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    protected Permission() {
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
