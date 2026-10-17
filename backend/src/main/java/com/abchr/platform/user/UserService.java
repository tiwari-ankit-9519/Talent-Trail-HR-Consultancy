package com.abchr.platform.user;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.auth.PasswordPolicy;
import com.abchr.platform.auth.RefreshTokenService;
import com.abchr.platform.role.AppRole;
import com.abchr.platform.role.RoleRepository;
import com.abchr.platform.role.SystemRoles;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import com.abchr.platform.user.UserDtos.CreateUserRequest;
import com.abchr.platform.user.UserDtos.RoleRef;
import com.abchr.platform.user.UserDtos.UpdateUserRequest;
import com.abchr.platform.user.UserDtos.UserResponse;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, UserStatus status, int page, int size) {
        Specification<AppUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<AppUser> result = userRepository.findAll(spec, Paging.of(page, size, Sort.by("fullName").ascending()));
        return PageResponse.of(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict(ErrorCodes.USER_EMAIL_EXISTS, "A user with this email already exists");
        }
        PasswordPolicy.validate(request.password(), email);
        Set<AppRole> roles = resolveRoles(request.roleIds());

        AppUser user = new AppUser(email, request.fullName().trim(), passwordEncoder.encode(request.password()));
        user.replaceRoles(roles);
        userRepository.save(user);

        Changes changes = Changes.create()
                .field("email", null, email)
                .field("fullName", null, user.getFullName())
                .field("roles", List.of(), user.roleNames())
                .field("mustChangePassword", null, true);
        auditService.record(AuditActions.USER_CREATED, AuditActions.ENTITY_USER, user.getId().toString(),
                "Created user " + email, changes.toJson());
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        AppUser user = find(id);
        VersionCheck.verify(request.version(), user.getVersion(), AppUser.class, id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflict(ErrorCodes.USER_EMAIL_EXISTS, "A user with this email already exists");
        }
        Set<AppRole> newRoles = resolveRoles(request.roleIds());
        boolean losesAdmin = isAdmin(user) && newRoles.stream().noneMatch(r -> SystemRoles.ADMIN_ID.equals(r.getId()));
        if (losesAdmin && user.isActive()) {
            requireAnotherActiveAdmin();
        }

        Changes changes = Changes.create()
                .field("email", user.getEmail(), email)
                .field("fullName", user.getFullName(), request.fullName().trim())
                .field("roles", user.roleNames(), newRoles.stream().map(AppRole::getName).sorted().toList());
        boolean rolesChanged = !user.roleNames().equals(newRoles.stream().map(AppRole::getName).sorted().toList());

        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.replaceRoles(newRoles);
        userRepository.saveAndFlush(user);

        if (rolesChanged) {
            refreshTokenService.revokeAllForUser(user.getId());
        }
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.USER_UPDATED, AuditActions.ENTITY_USER, id.toString(),
                    changes.summary("Updated user " + email), changes.toJson());
        }
        return toResponse(user);
    }

    @Transactional
    public UserResponse changeStatus(UUID id, boolean active) {
        AppUser user = find(id);
        if (user.isActive() == active) {
            return toResponse(user);
        }
        if (!active) {
            if (id.equals(Actor.current().id())) {
                throw ApiException.rule(ErrorCodes.USER_SELF_DEACTIVATE, "You cannot deactivate your own account");
            }
            if (isAdmin(user)) {
                requireAnotherActiveAdmin();
            }
        }
        user.setActive(active);
        userRepository.saveAndFlush(user);
        if (!active) {
            refreshTokenService.revokeAllForUser(id);
        }
        Changes changes = Changes.create().field("status", active ? UserStatus.INACTIVE : UserStatus.ACTIVE, user.getStatus());
        auditService.record(AuditActions.USER_STATUS_CHANGED, AuditActions.ENTITY_USER, id.toString(),
                (active ? "Activated user " : "Deactivated user ") + user.getEmail(), changes.toJson());
        return toResponse(user);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        AppUser user = find(id);
        PasswordPolicy.validate(newPassword, user.getEmail());
        user.setPassword(passwordEncoder.encode(newPassword), true, Instant.now());
        userRepository.saveAndFlush(user);
        refreshTokenService.revokeAllForUser(id);
        auditService.record(AuditActions.PASSWORD_RESET, AuditActions.ENTITY_USER, id.toString(),
                "Reset password for " + user.getEmail(), null);
    }

    private AppUser find(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.USER_NOT_FOUND, "User not found"));
    }

    private Set<AppRole> resolveRoles(Set<UUID> roleIds) {
        List<AppRole> found = roleRepository.findAllById(roleIds);
        if (found.size() != new HashSet<>(roleIds).size()) {
            throw ApiException.notFound(ErrorCodes.ROLE_NOT_FOUND, "One or more roles do not exist");
        }
        return new HashSet<>(found);
    }

    private boolean isAdmin(AppUser user) {
        return user.getRoles().stream().anyMatch(r -> SystemRoles.ADMIN_ID.equals(r.getId()));
    }

    private void requireAnotherActiveAdmin() {
        long activeAdmins = userRepository.countByStatusAndRoles_Id(UserStatus.ACTIVE, SystemRoles.ADMIN_ID);
        if (activeAdmins <= 1) {
            throw ApiException.rule(ErrorCodes.USER_LAST_ADMIN, "At least one active Admin must remain");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.isMustChangePassword(),
                user.isLockedAt(Instant.now()),
                user.getLastLoginAt(),
                user.getRoles().stream()
                        .map(r -> new RoleRef(r.getId(), r.getName()))
                        .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                        .toList(),
                user.getVersion(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
