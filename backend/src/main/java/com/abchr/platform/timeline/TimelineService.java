package com.abchr.platform.timeline;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditLog;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimelineService {

    public enum Subject { PLACEMENT, INVOICE, CANDIDATE, SUBMISSION, COMMISSION, PAYROLL_RUN }

    private static final int LIMIT = 300;
    private static final List<String> HIDDEN_ACTIONS = List.of(
            AuditActions.CANDIDATE_RESUME_DOWNLOADED, AuditActions.JOINING_EMAIL_DOWNLOADED);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<TimelineEvent> forSubject(Subject subject, UUID id) {
        authorize(subject);
        Map<String, Set<String>> targets = new LinkedHashMap<>();
        switch (subject) {
            case PLACEMENT -> expandPlacements(placementIdsOf(Subject.PLACEMENT, id), targets);
            case INVOICE -> expandPlacements(placementIdsOf(Subject.INVOICE, id), targets);
            case COMMISSION -> expandPlacements(placementIdsOf(Subject.COMMISSION, id), targets);
            case SUBMISSION -> {
                require(ids("select s.id from Submission s where s.id = :id", id), ErrorCodes.SUBMISSION_NOT_FOUND,
                        "Submission not found");
                expandSubmissions(Set.of(id), targets);
            }
            case CANDIDATE -> {
                require(ids("select c.id from Candidate c where c.id = :id", id), ErrorCodes.CANDIDATE_NOT_FOUND,
                        "Candidate not found");
                add(targets, AuditActions.ENTITY_CANDIDATE, List.of(id));
                expandSubmissions(new LinkedHashSet<>(ids("select s.id from Submission s where s.candidateId = :id", id)),
                        targets);
                expandPlacements(new LinkedHashSet<>(ids("select p.id from Placement p where p.candidateId = :id", id)),
                        targets);
            }
            case PAYROLL_RUN -> {
                require(ids("select r.id from PayrollRun r where r.id = :id", id), ErrorCodes.PAYROLL_NOT_FOUND,
                        "Payroll run not found");
                add(targets, AuditActions.ENTITY_PAYROLL_RUN, List.of(id));
            }
        }
        return query(targets);
    }

    private void authorize(Subject subject) {
        boolean allowed = switch (subject) {
            case PLACEMENT -> Permissions.hasAny("PLACEMENTS_VIEW", "PLACEMENTS_MANAGE");
            case INVOICE -> Permissions.hasAny("INVOICES_VIEW", "INVOICES_MANAGE");
            case COMMISSION -> Permissions.hasAny("COMMISSIONS_VIEW", "COMMISSIONS_MANAGE");
            case CANDIDATE -> Permissions.hasAny("CANDIDATES_VIEW", "CANDIDATES_MANAGE");
            case SUBMISSION -> Permissions.hasAny("SUBMISSIONS_VIEW", "SUBMISSIONS_MANAGE");
            case PAYROLL_RUN -> Permissions.hasAny("PAYROLL_MANAGE");
        };
        if (!allowed) {
            throw ApiException.forbidden(ErrorCodes.FORBIDDEN, "You do not have permission to do this");
        }
    }

    private Set<UUID> placementIdsOf(Subject subject, UUID id) {
        return switch (subject) {
            case INVOICE -> new LinkedHashSet<>(require(
                    ids("select i.placementId from Invoice i where i.id = :id", id), ErrorCodes.INVOICE_NOT_FOUND,
                    "Invoice not found"));
            case COMMISSION -> new LinkedHashSet<>(require(
                    ids("select c.placementId from Commission c where c.id = :id", id),
                    ErrorCodes.COMMISSION_NOT_FOUND, "Commission not found"));
            default -> new LinkedHashSet<>(require(
                    ids("select p.id from Placement p where p.id = :id", id), ErrorCodes.PLACEMENT_NOT_FOUND,
                    "Placement not found"));
        };
    }

    private void expandPlacements(Set<UUID> placementIds, Map<String, Set<String>> targets) {
        add(targets, AuditActions.ENTITY_PLACEMENT, placementIds);
        Set<UUID> submissionIds = new LinkedHashSet<>();
        for (UUID placementId : placementIds) {
            submissionIds.addAll(ids("select p.submissionId from Placement p where p.id = :id", placementId));
            add(targets, AuditActions.ENTITY_INVOICE,
                    ids("select i.id from Invoice i where i.placementId = :id", placementId));
            add(targets, AuditActions.ENTITY_COMMISSION,
                    ids("select c.id from Commission c where c.placementId = :id", placementId));
        }
        add(targets, AuditActions.ENTITY_SUBMISSION, submissionIds);
        for (UUID submissionId : submissionIds) {
            add(targets, AuditActions.ENTITY_INTERVIEW,
                    ids("select i.id from Interview i where i.submissionId = :id", submissionId));
        }
    }

    private void expandSubmissions(Set<UUID> submissionIds, Map<String, Set<String>> targets) {
        add(targets, AuditActions.ENTITY_SUBMISSION, submissionIds);
        Set<UUID> placementIds = new LinkedHashSet<>();
        for (UUID submissionId : submissionIds) {
            add(targets, AuditActions.ENTITY_INTERVIEW,
                    ids("select i.id from Interview i where i.submissionId = :id", submissionId));
            placementIds.addAll(ids("select p.id from Placement p where p.submissionId = :id", submissionId));
        }
        if (!placementIds.isEmpty()) {
            expandPlacements(placementIds, targets);
        }
    }

    private List<UUID> ids(String jpql, UUID id) {
        return entityManager.createQuery(jpql, UUID.class).setParameter("id", id).getResultList();
    }

    private List<UUID> require(List<UUID> ids, String code, String message) {
        if (ids.isEmpty()) {
            throw ApiException.notFound(code, message);
        }
        return ids;
    }

    private void add(Map<String, Set<String>> targets, String entityType, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Set<String> values = targets.computeIfAbsent(entityType, key -> new LinkedHashSet<>());
        ids.forEach(value -> values.add(value.toString()));
    }

    private List<TimelineEvent> query(Map<String, Set<String>> targets) {
        if (targets.isEmpty()) {
            return List.of();
        }
        StringBuilder jpql = new StringBuilder("select a from AuditLog a where a.action not in :hidden and (");
        List<String> types = new ArrayList<>(targets.keySet());
        for (int index = 0; index < types.size(); index++) {
            if (index > 0) {
                jpql.append(" or ");
            }
            jpql.append("(a.entityType = :t").append(index).append(" and a.entityId in :i").append(index).append(')');
        }
        jpql.append(") order by a.occurredAt desc");
        TypedQuery<AuditLog> typed = entityManager.createQuery(jpql.toString(), AuditLog.class);
        typed.setParameter("hidden", HIDDEN_ACTIONS);
        for (int index = 0; index < types.size(); index++) {
            typed.setParameter("t" + index, types.get(index));
            typed.setParameter("i" + index, targets.get(types.get(index)));
        }
        typed.setMaxResults(LIMIT);
        return typed.getResultList().stream().map(TimelineEvent::from).toList();
    }
}
