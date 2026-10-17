package com.abchr.platform.security;

import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.RequestErrors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOG = LogManager.getLogger(ProblemAccessDeniedHandler.class);

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        RequestErrors.mark(request, ErrorCodes.FORBIDDEN, exception);
        LOG.warn("Access denied: {} {} user={} reason=\"{}\"", request.getMethod(), request.getRequestURI(),
                Actor.current().email(), exception.getMessage());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"title\":\"Forbidden\",\"status\":403,"
                + "\"detail\":\"You do not have permission to do this\",\"code\":\"" + ErrorCodes.FORBIDDEN + "\"}");
    }
}
