package com.abchr.platform.audit;

import com.abchr.platform.shared.web.PageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @PersistenceContext
    private EntityManager entityManager;

    public record Filter(
            String action,
            String entityType,
            String entityId,
            String actorEmail,
            AuditOutcome outcome,
            Instant from,
            Instant to) {
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(Filter filter, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AuditLog> countRoot = countQuery.from(AuditLog.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countRoot, filter).toArray(Predicate[]::new));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        CriteriaQuery<AuditLog> dataQuery = cb.createQuery(AuditLog.class);
        Root<AuditLog> root = dataQuery.from(AuditLog.class);
        dataQuery.select(root)
                .where(predicates(cb, root, filter).toArray(Predicate[]::new))
                .orderBy(cb.desc(root.get("occurredAt")));
        TypedQuery<AuditLog> typed = entityManager.createQuery(dataQuery);
        typed.setFirstResult(safePage * safeSize);
        typed.setMaxResults(safeSize);
        List<AuditLogResponse> content = typed.getResultList().stream().map(AuditLogResponse::from).toList();

        int totalPages = (int) Math.ceil((double) total / safeSize);
        return new PageResponse<>(content, safePage, safeSize, total, totalPages);
    }

    private List<Predicate> predicates(CriteriaBuilder cb, Root<AuditLog> root, Filter filter) {
        List<Predicate> list = new ArrayList<>();
        if (hasText(filter.action())) {
            list.add(cb.equal(root.get("action"), filter.action()));
        }
        if (hasText(filter.entityType())) {
            list.add(cb.equal(root.get("entityType"), filter.entityType()));
        }
        if (hasText(filter.entityId())) {
            list.add(cb.equal(root.get("entityId"), filter.entityId()));
        }
        if (hasText(filter.actorEmail())) {
            list.add(cb.like(cb.lower(root.get("actorEmail")), "%" + filter.actorEmail().toLowerCase() + "%"));
        }
        if (filter.outcome() != null) {
            list.add(cb.equal(root.get("outcome"), filter.outcome()));
        }
        if (filter.from() != null) {
            list.add(cb.greaterThanOrEqualTo(root.<Instant>get("occurredAt"), filter.from()));
        }
        if (filter.to() != null) {
            list.add(cb.lessThan(root.<Instant>get("occurredAt"), filter.to()));
        }
        return list;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
