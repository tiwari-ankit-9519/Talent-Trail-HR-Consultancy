package com.abchr.platform.user;

import com.abchr.platform.role.AppRole;
import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<AppRole> roles = new HashSet<>();

    protected AppUser() {
    }

    public AppUser(String email, String fullName, String passwordHash) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void setActive(boolean active) {
        this.status = active ? UserStatus.ACTIVE : UserStatus.INACTIVE;
        if (active) {
            clearLockout();
        }
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Set<AppRole> getRoles() {
        return roles;
    }

    public void replaceRoles(Set<AppRole> newRoles) {
        roles.retainAll(newRoles);
        roles.addAll(newRoles);
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void clearExpiredLockout(Instant now) {
        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            clearLockout();
        }
    }

    public boolean registerFailedLogin(int maxAttempts, int lockMinutes, Instant now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plusSeconds(lockMinutes * 60L);
            failedLoginAttempts = 0;
            return true;
        }
        return false;
    }

    public void registerSuccessfulLogin(Instant now) {
        clearLockout();
        lastLoginAt = now;
    }

    public void setPassword(String newHash, boolean mustChange, Instant now) {
        this.passwordHash = newHash;
        this.mustChangePassword = mustChange;
        this.passwordChangedAt = now;
        clearLockout();
    }

    public Set<String> permissionCodes() {
        Set<String> codes = new HashSet<>();
        roles.forEach(role -> codes.addAll(role.getPermissionCodes()));
        return codes;
    }

    public List<String> roleNames() {
        return roles.stream().map(AppRole::getName).sorted().toList();
    }

    private void clearLockout() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }
}
