package com.abchr.platform.shared.web;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestErrors {

    public static final String ATTR_CODE = "app.error.code";
    public static final String ATTR_EXCEPTION = "app.error.exception";
    public static final String ATTR_ROOT = "app.error.root";

    private RequestErrors() {
    }

    public static void mark(HttpServletRequest request, String code, Throwable ex) {
        if (request == null) {
            return;
        }
        request.setAttribute(ATTR_CODE, code);
        if (ex != null) {
            request.setAttribute(ATTR_EXCEPTION, ex.getClass().getName());
            request.setAttribute(ATTR_ROOT, rootOf(ex).getClass().getName());
        }
    }

    public static Throwable rootOf(Throwable ex) {
        Throwable current = ex;
        int guard = 0;
        while (current.getCause() != null && current.getCause() != current && guard++ < 20) {
            current = current.getCause();
        }
        return current;
    }
}
