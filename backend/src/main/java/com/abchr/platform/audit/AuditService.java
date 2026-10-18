package com.abchr.platform.audit;

import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.RequestInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger LOG = LogManager.getLogger("audit");
    private static final Logger EVENTS = LogManager.getLogger("event");
    private static final Set<String> SECURITY_ACTIONS = Set.of(
            AuditActions.LOGIN_SUCCESS, AuditActions.LOGIN_FAILED, AuditActions.ACCOUNT_LOCKED, AuditActions.LOGOUT,
            AuditActions.TOKEN_REFRESHED, AuditActions.TOKEN_REUSE_DETECTED, AuditActions.PASSWORD_CHANGED,
            AuditActions.PASSWORD_RESET, AuditActions.ACCESS_DENIED, AuditActions.USER_BOOTSTRAPPED);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void record(String action, String entityType, String entityId, String summary, String detailsJson) {
        write(Actor.current(), action, entityType, entityId, summary, detailsJson, AuditOutcome.SUCCESS);
    }

    @Transactional
    public void recordAs(
            UUID actorId,
            String actorEmail,
            String action,
            String entityType,
            String entityId,
            String summary,
            String detailsJson,
            AuditOutcome outcome) {
        write(new Actor(actorId, actorEmail), action, entityType, entityId, summary, detailsJson, outcome);
    }

    private void write(
            Actor actor,
            String action,
            String entityType,
            String entityId,
            String summary,
            String detailsJson,
            AuditOutcome outcome) {
        RequestInfo request = RequestInfo.current();
        if (outcome == AuditOutcome.FAILURE) {
            LOG.warn("AUDIT action={} outcome={} entity={}:{} actor={} ip={} summary={}",
                    action, outcome, entityType, entityId, actor.email(), request.ipAddress(), summary);
        } else if (SECURITY_ACTIONS.contains(action)) {
            LOG.info("AUDIT action={} outcome={} entity={}:{} actor={} summary={}",
                    action, outcome, entityType, entityId, actor.email(), summary);
        } else {
            EVENTS.info("EVENT {} {}:{} by {} - {}", action, entityType, entityId, actor.email(), summary);
        }
        entityManager.persist(new AuditLog(
                actor.id(),
                cut(actor.email(), 255),
                cut(action, 60),
                cut(entityType, 60),
                cut(entityId, 100),
                cut(summary, 500),
                detailsJson,
                outcome,
                cut(request.ipAddress(), 64),
                cut(request.userAgent(), 300),
                cut(request.requestId(), 64)));
    }

    private static String cut(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
