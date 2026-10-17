package com.abchr.platform.shared.web;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditOutcome;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.shared.security.Actor;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(GlobalExceptionHandler.class);

    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex, HttpServletRequest request) {
        RequestErrors.mark(request, ex.getCode(), ex);
        String line = "Request rejected: {} {} status={} code={} reason=\"{}\"";
        if (ex.getStatus().is5xxServerError()) {
            LOG.error(line, request.getMethod(), request.getRequestURI(), ex.getStatus().value(), ex.getCode(),
                    ex.getMessage(), ex);
        } else {
            LOG.warn(line, request.getMethod(), request.getRequestURI(), ex.getStatus().value(), ex.getCode(),
                    ex.getMessage());
        }
        return problem(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleDenied(AccessDeniedException ex, HttpServletRequest request) {
        RequestErrors.mark(request, ErrorCodes.FORBIDDEN, ex);
        Actor actor = Actor.current();
        LOG.warn("Access denied: {} {} user={} reason=\"{}\"", request.getMethod(), request.getRequestURI(),
                actor.email(), ex.getMessage());
        auditService.recordAs(actor.id(), actor.email(), AuditActions.ACCESS_DENIED, AuditActions.ENTITY_REQUEST,
                null, "Denied " + request.getMethod() + " " + request.getRequestURI(), null, AuditOutcome.FAILURE);
        return problem(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "You do not have permission to do this");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        RequestErrors.mark(request, ErrorCodes.UNAUTHENTICATED, ex);
        LOG.warn("Authentication failed: {} {} type={} reason=\"{}\"", request.getMethod(), request.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED, "Authentication is required");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        RequestErrors.mark(request, ErrorCodes.STALE_VERSION, ex);
        LOG.warn("Stale update rejected: {} {} entity={} id={}", request.getMethod(), request.getRequestURI(),
                ex.getPersistentClassName(), ex.getIdentifier());
        return problem(HttpStatus.CONFLICT, ErrorCodes.STALE_VERSION,
                "The record was changed by someone else. Reload and try again.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        RequestErrors.mark(request, ErrorCodes.CONFLICT, ex);
        Throwable root = RequestErrors.rootOf(ex);
        String sqlState = root instanceof SQLException sql ? sql.getSQLState() : null;
        String constraint = ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException cve
                ? cve.getConstraintName()
                : null;
        LOG.warn("Data integrity violation: {} {} sqlState={} constraint={} root={}", request.getMethod(),
                request.getRequestURI(), sqlState, constraint, root.getClass().getName(), ex);
        return problem(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, "The change conflicts with existing data");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        RequestErrors.mark(request, ErrorCodes.INTERNAL_ERROR, ex);
        Throwable root = RequestErrors.rootOf(ex);
        LOG.error("Unhandled exception: {} {} exception={} root={} rootMessage=\"{}\"", request.getMethod(),
                request.getRequestURI(), ex.getClass().getName(), root.getClass().getName(), root.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getCode() == null ? "Invalid" : error.getCode()));
        HttpServletRequest servlet = servletOf(request);
        RequestErrors.mark(servlet, ErrorCodes.VALIDATION_FAILED, ex);
        LOG.warn("Validation failed: {} {} target={} fields={}", method(servlet), uri(servlet),
                ex.getBindingResult().getObjectName(), fields);
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Validation failed");
        detail.setProperty("fields", fields);
        return ResponseEntity.badRequest().body(detail);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpServletRequest servlet = servletOf(request);
        RequestErrors.mark(servlet, "HTTP_" + statusCode.value(), ex);
        if (statusCode.is5xxServerError()) {
            LOG.error("Request failed: {} {} status={} exception={} reason=\"{}\"", method(servlet), uri(servlet),
                    statusCode.value(), ex.getClass().getName(), ex.getMessage(), ex);
        } else {
            LOG.warn("Request rejected: {} {} status={} exception={} reason=\"{}\"", method(servlet), uri(servlet),
                    statusCode.value(), ex.getClass().getName(), ex.getMessage());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static HttpServletRequest servletOf(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest() : null;
    }

    private static String method(HttpServletRequest request) {
        return request == null ? "?" : request.getMethod();
    }

    private static String uri(HttpServletRequest request) {
        return request == null ? "?" : request.getRequestURI();
    }

    private ProblemDetail problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setProperty("code", code);
        String requestId = ThreadContext.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            detail.setProperty("requestId", requestId);
        }
        return detail;
    }
}
