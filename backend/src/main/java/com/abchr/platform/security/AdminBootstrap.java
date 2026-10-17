package com.abchr.platform.security;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditOutcome;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.auth.PasswordPolicy;
import com.abchr.platform.role.AppRole;
import com.abchr.platform.role.RoleRepository;
import com.abchr.platform.role.SystemRoles;
import com.abchr.platform.shared.config.AppProperties;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOG = LogManager.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AppProperties properties;

    public AdminBootstrap(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            AppProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        String email = properties.auth().bootstrapAdminEmail();
        String password = properties.auth().bootstrapAdminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "No users exist yet. Set BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD to create the first admin.");
        }
        String normalized = email.trim().toLowerCase();
        PasswordPolicy.validate(password, normalized);

        AppRole adminRole = roleRepository.findById(SystemRoles.ADMIN_ID)
                .orElseThrow(() -> new IllegalStateException("The Admin role is missing from the database"));
        AppUser admin = new AppUser(normalized, "Administrator", passwordEncoder.encode(password));
        admin.replaceRoles(Set.of(adminRole));
        userRepository.save(admin);

        auditService.recordAs(null, "system", AuditActions.USER_BOOTSTRAPPED, AuditActions.ENTITY_USER,
                admin.getId().toString(), "Created the first admin account " + normalized, null,
                AuditOutcome.SUCCESS);
        LOG.info("Created first admin account {}", normalized);
    }
}
