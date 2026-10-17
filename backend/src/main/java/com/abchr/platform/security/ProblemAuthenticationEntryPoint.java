package com.abchr.platform.security;

import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.RequestErrors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOG = LogManager.getLogger(ProblemAuthenticationEntryPoint.class);

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        RequestErrors.mark(request, ErrorCodes.UNAUTHENTICATED, exception);
        LOG.warn("Unauthenticated request: {} {} type={} reason=\"{}\" hasAuthorizationHeader={}", request.getMethod(),
                request.getRequestURI(), exception.getClass().getSimpleName(), exception.getMessage(),
                request.getHeader("Authorization") != null);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401,"
                + "\"detail\":\"Authentication is required\",\"code\":\"" + ErrorCodes.UNAUTHENTICATED + "\"}");
    }
}
