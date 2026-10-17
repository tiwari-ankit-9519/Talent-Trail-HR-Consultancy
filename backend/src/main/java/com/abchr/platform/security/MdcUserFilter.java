package com.abchr.platform.security;

import com.abchr.platform.shared.security.Actor;
import com.abchr.platform.shared.web.RequestIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.filter.OncePerRequestFilter;

public class MdcUserFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Actor actor = Actor.current();
        if (actor != Actor.SYSTEM) {
            if (actor.id() != null) {
                ThreadContext.put(RequestIdFilter.MDC_USER_KEY, actor.id().toString());
            }
            ThreadContext.put(RequestIdFilter.MDC_USER_EMAIL_KEY, actor.email());
        }
        chain.doFilter(request, response);
    }
}
