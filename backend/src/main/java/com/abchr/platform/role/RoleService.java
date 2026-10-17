package com.abchr.platform.role;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.role.RoleDtos.PermissionResponse;
import com.abchr.platform.role.RoleDtos.RoleResponse;
import com.abchr.platform.role.RoleDtos.SaveRoleRequest;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> permissions() {
        return permissionRepository.findAllByOrderByCodeAsc().stream()
                .map(p -> new PermissionResponse(p.getCode(), p.getDescription()))
                .toList();
    }

    @Transactional
    public RoleResponse create(SaveRoleRequest request) {
        String name = request.name().trim();
        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict(ErrorCodes.ROLE_NAME_EXISTS, "A role with this name already exists");
        }
        Set<String> permissions = validatedPermissions(request.permissions());
        AppRole role = new AppRole(name, blankToNull(request.description()));
        role.replacePermissions(permissions);
        roleRepository.save(role);

        Changes changes = Changes.create()
                .field("name", null, role.getName())
                .field("description", null, role.getDescription())
                .field("permissions", List.of(), Changes.sortedNames(permissions));
        auditService.record(AuditActions.ROLE_CREATED, AuditActions.ENTITY_ROLE, role.getId().toString(),
                "Created role " + role.getName(), changes.toJson());
        return toResponse(role);
    }

    @Transactional
    public RoleResponse update(UUID id, SaveRoleRequest request) {
        AppRole role = find(id);
        VersionCheck.verify(request.version(), role.getVersion(), AppRole.class, id);
        String name = request.name().trim();
        Set<String> permissions = validatedPermissions(request.permissions());

        if (role.isSystemRole() && !role.getName().equals(name)) {
            throw ApiException.rule(ErrorCodes.ROLE_SYSTEM_PROTECTED, "System roles cannot be renamed");
        }
        if (roleRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict(ErrorCodes.ROLE_NAME_EXISTS, "A role with this name already exists");
        }
        if (SystemRoles.ADMIN_ID.equals(id) && !permissions.equals(role.getPermissionCodes())) {
            throw ApiException.rule(ErrorCodes.ROLE_SYSTEM_PROTECTED, "The Admin role always keeps every permission");
        }

        Changes changes = Changes.create()
                .field("name", role.getName(), name)
                .field("description", role.getDescription(), blankToNull(request.description()))
                .field("permissions", Changes.sortedNames(role.getPermissionCodes()), Changes.sortedNames(permissions));
        role.setName(name);
        role.setDescription(blankToNull(request.description()));
        role.replacePermissions(permissions);
        roleRepository.saveAndFlush(role);

        if (!changes.isEmpty()) {
            auditService.record(AuditActions.ROLE_UPDATED, AuditActions.ENTITY_ROLE, id.toString(),
                    changes.summary("Updated role " + name), changes.toJson());
        }
        return toResponse(role);
    }

    @Transactional
    public void delete(UUID id) {
        AppRole role = find(id);
        if (role.isSystemRole()) {
            throw ApiException.rule(ErrorCodes.ROLE_SYSTEM_PROTECTED, "System roles cannot be deleted");
        }
        if (userRepository.countByRoles_Id(id) > 0) {
            throw ApiException.conflict(ErrorCodes.ROLE_IN_USE, "Remove this role from all users before deleting it");
        }
        Changes changes = Changes.create()
                .field("name", role.getName(), null)
                .field("description", role.getDescription(), null)
                .field("permissions", Changes.sortedNames(role.getPermissionCodes()), List.of());
        roleRepository.delete(role);
        auditService.record(AuditActions.ROLE_DELETED, AuditActions.ENTITY_ROLE, id.toString(),
                "Deleted role " + role.getName(), changes.toJson());
    }

    private AppRole find(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.ROLE_NOT_FOUND, "Role not found"));
    }

    private Set<String> validatedPermissions(Set<String> requested) {
        Set<String> known = permissionRepository.findAll().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        Set<String> unknown = new HashSet<>(requested);
        unknown.removeAll(known);
        if (!unknown.isEmpty()) {
            throw ApiException.rule(ErrorCodes.PERMISSION_UNKNOWN, "Unknown permissions: " + String.join(", ", unknown));
        }
        return new HashSet<>(requested);
    }

    private RoleResponse toResponse(AppRole role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                Changes.sortedNames(role.getPermissionCodes()),
                userRepository.countByRoles_Id(role.getId()),
                role.getVersion());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
