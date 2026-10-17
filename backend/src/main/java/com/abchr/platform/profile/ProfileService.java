package com.abchr.platform.profile;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.profile.ProfileDtos.ContactRequest;
import com.abchr.platform.profile.ProfileDtos.ManagerOption;
import com.abchr.platform.profile.ProfileDtos.Sensitive;
import com.abchr.platform.profile.ProfileDtos.SensitiveRequest;
import com.abchr.platform.profile.ProfileDtos.Summary;
import com.abchr.platform.profile.ProfileDtos.UpdateRequest;
import com.abchr.platform.profile.ProfileDtos.View;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.user.AppUser;
import com.abchr.platform.user.UserRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private static final int EXPIRY_WARNING_DAYS = 30;

    private final EmployeeProfileRepository profiles;
    private final EmployeeDocumentRepository documents;
    private final UserRepository users;
    private final FieldCrypto crypto;
    private final AuditService auditService;

    public ProfileService(
            EmployeeProfileRepository profiles,
            EmployeeDocumentRepository documents,
            UserRepository users,
            FieldCrypto crypto,
            AuditService auditService) {
        this.profiles = profiles;
        this.documents = documents;
        this.users = users;
        this.crypto = crypto;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Summary> list(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Map<UUID, EmployeeProfile> byUser = profiles.findAll().stream()
                .collect(Collectors.toMap(EmployeeProfile::getUserId, Function.identity()));
        Map<UUID, List<EmployeeDocument>> docsByUser = documents.findAll().stream()
                .collect(Collectors.groupingBy(d -> d.userId));
        Map<UUID, AppUser> allUsers = users.findAll().stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
        LocalDate today = LocalDate.now();
        return allUsers.values().stream()
                .sorted(Comparator.comparing(u -> u.getFullName().toLowerCase(Locale.ROOT)))
                .map(user -> {
                    EmployeeProfile p = byUser.get(user.getId());
                    List<EmployeeDocument> docs = docsByUser.getOrDefault(user.getId(), List.of());
                    int expiring = (int) docs.stream()
                            .filter(d -> d.expiresOn != null
                                    && !d.expiresOn.isAfter(today.plusDays(EXPIRY_WARNING_DAYS)))
                            .count();
                    AppUser manager = p == null || p.managerUserId == null ? null : allUsers.get(p.managerUserId);
                    boolean bank = p != null && p.bankAccountEnc != null && p.bankIfsc != null;
                    return new Summary(user.getId(), user.getFullName(), user.getEmail(), user.isActive(),
                            p == null ? null : p.employeeCode, p == null ? null : p.designation,
                            p == null ? null : p.department, p == null ? null : p.joiningDate,
                            manager == null ? null : manager.getFullName(), bank, docs.size(), expiring);
                })
                .filter(s -> needle.isEmpty() || matches(s, needle))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagerOption> managerOptions() {
        return users.findAll(Sort.by("fullName")).stream()
                .filter(AppUser::isActive)
                .map(u -> new ManagerOption(u.getId(), u.getFullName()))
                .toList();
    }

    @Transactional
    public View get(UUID userId) {
        boolean self = isSelf(userId);
        if (!self && !Permissions.hasAny("PROFILES_VIEW", "PROFILES_MANAGE")) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "You do not have permission to do this");
        }
        if (self && !Permissions.hasAny("PROFILE_SELF", "PROFILES_VIEW", "PROFILES_MANAGE")) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "You do not have permission to do this");
        }
        AppUser user = requireUser(userId);
        EmployeeProfile profile = profiles.findById(userId).orElse(null);
        boolean viewSensitive = Permissions.hasAny("PROFILES_SENSITIVE");
        if (viewSensitive && !self && profile != null && hasSensitive(profile)) {
            auditService.record(AuditActions.PROFILE_SENSITIVE_VIEWED, AuditActions.ENTITY_PROFILE,
                    userId.toString(), "Viewed bank and tax details of " + user.getFullName(), null);
        }
        return toView(user, profile, self);
    }

    @Transactional
    public View update(UUID userId, UpdateRequest request) {
        AppUser user = requireUser(userId);
        EmployeeProfile profile = profiles.findById(userId).orElseGet(() -> EmployeeProfile.forUser(userId));
        if (profile.getVersion() != null) {
            VersionCheck.verify(request.version(), profile.getVersion(), EmployeeProfile.class, userId);
        }
        String code = clean(request.employeeCode());
        if (code != null && profiles.existsByEmployeeCodeIgnoreCaseAndUserIdNot(code, userId)) {
            throw ApiException.conflict(ErrorCodes.PROFILE_EMPLOYEE_CODE_TAKEN,
                    "Another employee already has this employee code");
        }
        UUID managerId = request.managerUserId();
        if (managerId != null) {
            if (managerId.equals(userId)) {
                throw ApiException.rule(ErrorCodes.PROFILE_MANAGER_INVALID, "An employee cannot report to themselves");
            }
            AppUser manager = requireUser(managerId);
            if (!manager.isActive()) {
                throw ApiException.rule(ErrorCodes.PROFILE_MANAGER_INVALID, "The reporting manager is inactive");
            }
        }
        Changes changes = Changes.create()
                .field("employeeCode", profile.employeeCode, code)
                .field("employmentType", profile.employmentType, request.employmentType())
                .field("designation", profile.designation, clean(request.designation()))
                .field("department", profile.department, clean(request.department()))
                .field("joiningDate", profile.joiningDate, request.joiningDate())
                .field("managerUserId", profile.managerUserId, managerId)
                .field("phone", profile.phone, clean(request.phone()))
                .field("dateOfBirth", profile.dateOfBirth, request.dateOfBirth())
                .field("city", profile.city, clean(request.city()))
                .field("state", profile.state, clean(request.state()))
                .field("pincode", profile.pincode, clean(request.pincode()));
        profile.employeeCode = code;
        profile.employmentType = request.employmentType();
        profile.designation = clean(request.designation());
        profile.department = clean(request.department());
        profile.joiningDate = request.joiningDate();
        profile.managerUserId = managerId;
        profile.phone = clean(request.phone());
        profile.dateOfBirth = request.dateOfBirth();
        profile.addressLine = clean(request.addressLine());
        profile.city = clean(request.city());
        profile.state = clean(request.state());
        profile.pincode = clean(request.pincode());
        profile.emergencyName = clean(request.emergencyName());
        profile.emergencyPhone = clean(request.emergencyPhone());
        profile.emergencyRelation = clean(request.emergencyRelation());
        EmployeeProfile saved = profiles.saveAndFlush(profile);
        auditService.record(AuditActions.PROFILE_UPDATED, AuditActions.ENTITY_PROFILE, userId.toString(),
                changes.summary("Updated profile of " + user.getFullName()), changes.toJson());
        return toView(user, saved, isSelf(userId));
    }

    @Transactional
    public View updateContact(ContactRequest request) {
        UUID userId = Actor.current().id();
        AppUser user = requireUser(userId);
        EmployeeProfile profile = profiles.findById(userId).orElseGet(() -> EmployeeProfile.forUser(userId));
        if (profile.getVersion() != null) {
            VersionCheck.verify(request.version(), profile.getVersion(), EmployeeProfile.class, userId);
        }
        Changes changes = Changes.create()
                .field("phone", profile.phone, clean(request.phone()))
                .field("city", profile.city, clean(request.city()))
                .field("state", profile.state, clean(request.state()))
                .field("pincode", profile.pincode, clean(request.pincode()))
                .field("emergencyName", profile.emergencyName, clean(request.emergencyName()))
                .field("emergencyPhone", profile.emergencyPhone, clean(request.emergencyPhone()))
                .field("emergencyRelation", profile.emergencyRelation, clean(request.emergencyRelation()));
        profile.phone = clean(request.phone());
        profile.addressLine = clean(request.addressLine());
        profile.city = clean(request.city());
        profile.state = clean(request.state());
        profile.pincode = clean(request.pincode());
        profile.emergencyName = clean(request.emergencyName());
        profile.emergencyPhone = clean(request.emergencyPhone());
        profile.emergencyRelation = clean(request.emergencyRelation());
        EmployeeProfile saved = profiles.saveAndFlush(profile);
        auditService.record(AuditActions.PROFILE_CONTACT_UPDATED, AuditActions.ENTITY_PROFILE, userId.toString(),
                changes.summary("Updated own contact details"), changes.toJson());
        return toView(user, saved, true);
    }

    @Transactional
    public View updateSensitive(UUID userId, SensitiveRequest request) {
        AppUser user = requireUser(userId);
        EmployeeProfile profile = profiles.findById(userId).orElseGet(() -> EmployeeProfile.forUser(userId));
        if (profile.getVersion() != null) {
            VersionCheck.verify(request.version(), profile.getVersion(), EmployeeProfile.class, userId);
        }
        String account = clean(request.bankAccountNumber());
        String pan = clean(request.pan());
        String uan = clean(request.uan());
        String ifsc = clean(request.bankIfsc());
        pan = pan == null ? null : pan.toUpperCase(Locale.ROOT);
        ifsc = ifsc == null ? null : ifsc.toUpperCase(Locale.ROOT);
        Changes changes = Changes.create()
                .field("bankAccountHolder", profile.bankAccountHolder, clean(request.bankAccountHolder()))
                .field("bankName", profile.bankName, clean(request.bankName()))
                .field("bankIfsc", profile.bankIfsc, ifsc)
                .field("bankAccountNumber", FieldCrypto.mask(crypto.decrypt(profile.bankAccountEnc)),
                        FieldCrypto.mask(account))
                .field("pan", FieldCrypto.mask(crypto.decrypt(profile.panEnc)), FieldCrypto.mask(pan))
                .field("uan", FieldCrypto.mask(crypto.decrypt(profile.uanEnc)), FieldCrypto.mask(uan));
        profile.bankAccountHolder = clean(request.bankAccountHolder());
        profile.bankName = clean(request.bankName());
        profile.bankIfsc = ifsc;
        profile.bankAccountEnc = crypto.encrypt(account);
        profile.panEnc = crypto.encrypt(pan);
        profile.uanEnc = crypto.encrypt(uan);
        EmployeeProfile saved = profiles.saveAndFlush(profile);
        auditService.record(AuditActions.PROFILE_SENSITIVE_UPDATED, AuditActions.ENTITY_PROFILE, userId.toString(),
                changes.summary("Updated bank and tax details of " + user.getFullName()), changes.toJson());
        return toView(user, saved, isSelf(userId));
    }

    boolean isSelf(UUID userId) {
        return userId.equals(Actor.current().id());
    }

    AppUser requireUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.USER_NOT_FOUND, "User not found"));
    }

    private View toView(AppUser user, EmployeeProfile p, boolean self) {
        boolean manage = Permissions.hasAny("PROFILES_MANAGE");
        boolean sensitivePermission = Permissions.hasAny("PROFILES_SENSITIVE");
        Sensitive sensitive = sensitivePermission || self ? toSensitive(p, !sensitivePermission) : null;
        AppUser manager = p == null || p.managerUserId == null ? null : users.findById(p.managerUserId).orElse(null);
        return new View(user.getId(), user.getFullName(), user.getEmail(), user.isActive(), user.roleNames(),
                p == null ? null : p.employeeCode, p == null ? EmploymentType.FULL_TIME : p.employmentType,
                p == null ? null : p.designation, p == null ? null : p.department,
                p == null ? null : p.joiningDate, p == null ? null : p.managerUserId,
                manager == null ? null : manager.getFullName(), p == null ? null : p.phone,
                p == null ? null : p.dateOfBirth, p == null ? null : p.addressLine, p == null ? null : p.city,
                p == null ? null : p.state, p == null ? null : p.pincode, p == null ? null : p.emergencyName,
                p == null ? null : p.emergencyPhone, p == null ? null : p.emergencyRelation, sensitive, manage,
                self, sensitivePermission, manage, p == null ? null : p.getVersion());
    }

    private Sensitive toSensitive(EmployeeProfile p, boolean masked) {
        if (p == null) {
            return new Sensitive(null, null, null, null, null, null, masked);
        }
        String account = crypto.decrypt(p.bankAccountEnc);
        String pan = crypto.decrypt(p.panEnc);
        String uan = crypto.decrypt(p.uanEnc);
        return new Sensitive(p.bankAccountHolder, p.bankName,
                masked ? FieldCrypto.mask(account) : account, p.bankIfsc,
                masked ? FieldCrypto.mask(pan) : pan, masked ? FieldCrypto.mask(uan) : uan, masked);
    }

    private static boolean hasSensitive(EmployeeProfile p) {
        return p.bankAccountEnc != null || p.panEnc != null || p.uanEnc != null;
    }

    private static boolean matches(Summary s, String needle) {
        return contains(s.fullName(), needle) || contains(s.email(), needle) || contains(s.employeeCode(), needle)
                || contains(s.designation(), needle) || contains(s.department(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
