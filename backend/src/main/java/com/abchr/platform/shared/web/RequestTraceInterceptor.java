package com.abchr.platform.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class RequestTraceInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LogManager.getLogger(RequestTraceInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (LOG.isDebugEnabled()) {
            String query = request.getQueryString();
            LOG.debug("REQUEST {} {}{} handler={} contentType={} contentLength={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    query == null ? "" : "?" + query,
                    describe(handler),
                    request.getContentType(),
                    request.getContentLengthLong());
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("HANDLED {} {} handler={} status={}",
                    request.getMethod(), request.getRequestURI(), describe(handler), response.getStatus());
        }
    }

    private static String describe(Object handler) {
        if (handler instanceof HandlerMethod method) {
            return method.getBeanType().getSimpleName() + "#" + method.getMethod().getName();
        }
        return handler == null ? "none" : handler.getClass().getSimpleName();
    }
}
