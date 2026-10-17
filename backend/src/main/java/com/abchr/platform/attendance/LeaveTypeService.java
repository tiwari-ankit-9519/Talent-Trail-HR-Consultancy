package com.abchr.platform.attendance;

import com.abchr.platform.attendance.AttendanceDtos.LeaveTypeRequest;
import com.abchr.platform.attendance.AttendanceDtos.LeaveTypeView;
import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveTypeService {

    private final LeaveTypeRepository typeRepository;
    private final LeaveApplicationRepository applicationRepository;
    private final AuditService auditService;

    public LeaveTypeService(
            LeaveTypeRepository typeRepository,
            LeaveApplicationRepository applicationRepository,
            AuditService auditService) {
        this.typeRepository = typeRepository;
        this.applicationRepository = applicationRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeView> list(boolean activeOnly) {
        List<LeaveType> rows = activeOnly
                ? typeRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                : typeRepository.findAllByOrderBySortOrderAscNameAsc();
        return rows.stream().map(LeaveTypeService::toView).toList();
    }

    @Transactional
    public LeaveTypeView create(LeaveTypeRequest request) {
        String name = request.name().trim();
        if (typeRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict(ErrorCodes.LEAVE_TYPE_EXISTS, "A leave type named " + name + " already exists");
        }
        LeaveType type = LeaveType.create();
        type.name = name;
        type.yearlyDays = days(request);
        type.unpaid = request.unpaid();
        type.active = true;
        type.sortOrder = (int) typeRepository.count() + 1;
        typeRepository.saveAndFlush(type);
        auditService.record(AuditActions.LEAVE_TYPE_CREATED, AuditActions.ENTITY_LEAVE_TYPE, type.getId().toString(),
                "Created leave type " + name, null);
        return toView(type);
    }

    @Transactional
    public LeaveTypeView update(UUID id, LeaveTypeRequest request) {
        LeaveType type = find(id);
        VersionCheck.verify(request.version(), type.getVersion(), LeaveType.class, id);
        String name = request.name().trim();
        if (typeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict(ErrorCodes.LEAVE_TYPE_EXISTS, "A leave type named " + name + " already exists");
        }
        BigDecimal yearly = days(request);
        Changes changes = Changes.create()
                .field("name", type.name, name)
                .field("yearlyDays", type.yearlyDays.doubleValue(), yearly.doubleValue())
                .field("unpaid", type.unpaid, request.unpaid())
                .field("active", type.active, request.active());
        type.name = name;
        type.yearlyDays = yearly;
        type.unpaid = request.unpaid();
        type.active = request.active();
        typeRepository.saveAndFlush(type);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.LEAVE_TYPE_UPDATED, AuditActions.ENTITY_LEAVE_TYPE, id.toString(),
                    changes.summary("Updated leave type " + name), changes.toJson());
        }
        return toView(type);
    }

    @Transactional
    public void delete(UUID id) {
        LeaveType type = find(id);
        if (applicationRepository.existsByLeaveTypeId(id)) {
            throw ApiException.conflict(ErrorCodes.LEAVE_TYPE_IN_USE,
                    "This leave type has applications. Deactivate it instead.");
        }
        typeRepository.delete(type);
        auditService.record(AuditActions.LEAVE_TYPE_DELETED, AuditActions.ENTITY_LEAVE_TYPE, id.toString(),
                "Deleted leave type " + type.name, null);
    }

    LeaveType find(UUID id) {
        return typeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.LEAVE_TYPE_NOT_FOUND, "Leave type not found"));
    }

    private static BigDecimal days(LeaveTypeRequest request) {
        double rounded = Math.round(request.yearlyDays() * 2) / 2.0;
        return BigDecimal.valueOf(rounded);
    }

    private static LeaveTypeView toView(LeaveType type) {
        return new LeaveTypeView(type.getId(), type.name, type.yearlyDays.doubleValue(), type.unpaid, type.active,
                type.sortOrder, type.getVersion());
    }
}
