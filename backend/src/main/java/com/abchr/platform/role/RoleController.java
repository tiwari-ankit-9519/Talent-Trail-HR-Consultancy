package com.abchr.platform.role;

import com.abchr.platform.role.RoleDtos.PermissionResponse;
import com.abchr.platform.role.RoleDtos.RoleResponse;
import com.abchr.platform.role.RoleDtos.SaveRoleRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('ROLES_VIEW','ROLES_MANAGE','USERS_VIEW','USERS_MANAGE')")
    public List<RoleResponse> list() {
        return roleService.list();
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_VIEW','ROLES_MANAGE')")
    public RoleResponse get(@PathVariable UUID id) {
        return roleService.get(id);
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public RoleResponse create(@Valid @RequestBody SaveRoleRequest request) {
        return roleService.create(request);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody SaveRoleRequest request) {
        return roleService.update(id, request);
    }

    @DeleteMapping("/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public void delete(@PathVariable UUID id) {
        roleService.delete(id);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAnyAuthority('ROLES_VIEW','ROLES_MANAGE')")
    public List<PermissionResponse> permissions() {
        return roleService.permissions();
    }
}
