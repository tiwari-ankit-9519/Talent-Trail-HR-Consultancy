package com.abchr.platform.role;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_role")
public class AppRole extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission_code", nullable = false, length = 60)
    private Set<String> permissionCodes = new HashSet<>();

    protected AppRole() {
    }

    public AppRole(String name, String description) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.systemRole = false;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public Set<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void replacePermissions(Set<String> codes) {
        permissionCodes.retainAll(codes);
        permissionCodes.addAll(codes);
    }
}
