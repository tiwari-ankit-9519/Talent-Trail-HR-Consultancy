package com.abchr.platform.imports;

import static com.abchr.platform.imports.ImportSupport.text;

import com.abchr.platform.imports.ImportDtos.Credential;
import com.abchr.platform.imports.ImportDtos.ImportReport;
import com.abchr.platform.imports.ImportDtos.RowResult;
import com.abchr.platform.profile.EmploymentType;
import com.abchr.platform.profile.ProfileDtos.UpdateRequest;
import com.abchr.platform.profile.ProfileService;
import com.abchr.platform.user.UserDtos.CreateUserRequest;
import com.abchr.platform.user.UserDtos.UserResponse;
import com.abchr.platform.user.UserRepository;
import com.abchr.platform.user.UserService;
import jakarta.validation.Validator;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class EmployeeImportHandler implements ImportHandler {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "@#$%&*!?";
    private static final Map<String, String> LABELS = Map.of("phone", "Phone");

    private final UserService userService;
    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final JdbcTemplate jdbc;
    private final Validator validator;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();

    EmployeeImportHandler(
            UserService userService,
            UserRepository userRepository,
            ProfileService profileService,
            JdbcTemplate jdbc,
            Validator validator,
            PlatformTransactionManager transactionManager) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.jdbc = jdbc;
        this.validator = validator;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public String type() {
        return "employees";
    }

    @Override
    public String[] requiredPermissions() {
        return new String[] {"USERS_MANAGE", "PROFILES_MANAGE"};
    }

    @Override
    public ImportReport process(List<Map<String, String>> rows, boolean commit) {
        List<RowResult> results = new ArrayList<>();
        List<Credential> credentials = new ArrayList<>();
        Map<String, Integer> seenEmails = new HashMap<>();
        Set<String> seenCodes = new HashSet<>();
        Map<String, UUID> roles = roleIds();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            List<String> errors = ImportSupport.list();
            String name = text(row, "fullName");
            String email = text(row, "email");
            String label = name == null ? email : name;
            if (name == null) {
                errors.add("Full name is required");
            }
            if (email == null || !EMAIL.matcher(email).matches()) {
                errors.add("Email must be a valid email address");
            }
            if (email != null && EMAIL.matcher(email).matches()) {
                String key = email.toLowerCase();
                Integer first = seenEmails.putIfAbsent(key, i + 2);
                if (first != null) {
                    results.add(ImportSupport.result(i, label, ImportSupport.DUPLICATE,
                            List.of("Same email as row " + first + " in this file")));
                    continue;
                }
                if (userRepository.existsByEmailIgnoreCase(key)) {
                    results.add(ImportSupport.result(i, label, ImportSupport.DUPLICATE,
                            List.of("A user with this email already exists")));
                    continue;
                }
            }
            Set<UUID> roleIds = new HashSet<>();
            String roleText = text(row, "role");
            if (roleText == null) {
                errors.add("Role is required");
            } else {
                for (String part : roleText.split("[;|]")) {
                    UUID id = roles.get(part.strip().toLowerCase());
                    if (id == null) {
                        errors.add("Unknown role \"" + part.strip() + "\"");
                    } else {
                        roleIds.add(id);
                    }
                }
            }
            String code = text(row, "employeeCode");
            if (code != null && (!seenCodes.add(code.toLowerCase()) || codeTaken(code))) {
                errors.add("Employee code " + code + " is already used");
            }
            EmploymentType type = ImportSupport.enumValue(row, "employmentType", "Employment type",
                    EmploymentType.class, EmploymentType.FULL_TIME, errors);
            LocalDate joining = ImportSupport.date(row, "joiningDate", "Joining date", errors);
            UpdateRequest profile = new UpdateRequest(code, type, text(row, "designation"), text(row, "department"),
                    joining, null, text(row, "phone"), null, null, null, null, null, null, null, null, null);
            ImportSupport.violations(validator, profile, LABELS, errors);
            if (!errors.isEmpty()) {
                results.add(ImportSupport.result(i, label, ImportSupport.INVALID, errors));
                continue;
            }
            if (!commit) {
                results.add(ImportSupport.result(i, label, ImportSupport.VALID, errors));
                continue;
            }
            String password = generatePassword();
            try {
                tx.executeWithoutResult(status -> {
                    UserResponse user = userService.create(new CreateUserRequest(email, name, password, roleIds));
                    profileService.update(user.id(), profile);
                });
                credentials.add(new Credential(email.toLowerCase(), name, password));
                results.add(ImportSupport.result(i, label, ImportSupport.IMPORTED, errors));
            } catch (RuntimeException ex) {
                results.add(ImportSupport.result(i, label, ImportSupport.INVALID,
                        List.of(ex.getMessage() == null ? "Could not import this row" : ex.getMessage())));
            }
        }
        return ImportSupport.report(type(), commit, results, credentials);
    }

    private Map<String, UUID> roleIds() {
        Map<String, UUID> out = new HashMap<>();
        jdbc.query("select id, name from app_role",
                rs -> {
                    out.put(rs.getString("name").toLowerCase(), rs.getObject("id", UUID.class));
                });
        return out;
    }

    private boolean codeTaken(String code) {
        Integer count = jdbc.queryForObject(
                "select count(*) from employee_profile where lower(employee_code) = lower(?)", Integer.class, code);
        return count != null && count > 0;
    }

    String generatePassword() {
        String all = UPPER + LOWER + DIGITS + SYMBOLS;
        List<Character> chars = new ArrayList<>();
        chars.add(UPPER.charAt(random.nextInt(UPPER.length())));
        chars.add(LOWER.charAt(random.nextInt(LOWER.length())));
        chars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        chars.add(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
        while (chars.size() < 16) {
            chars.add(all.charAt(random.nextInt(all.length())));
        }
        java.util.Collections.shuffle(chars, random);
        StringBuilder out = new StringBuilder();
        chars.forEach(out::append);
        return out.toString();
    }
}
