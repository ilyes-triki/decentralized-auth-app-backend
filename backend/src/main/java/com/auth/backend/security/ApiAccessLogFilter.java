package com.auth.backend.security;

import com.auth.backend.service.ApiAccessLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Persists one row per completed HTTP request under /api (Step 7).
 * Skips frequent admin health polling to keep the table meaningful.
 * Registered after {@link JwtAuthenticationFilter} so principal is available when authenticated.
 */
public class ApiAccessLogFilter extends OncePerRequestFilter {

    private static final String START_ATTR = ApiAccessLogFilter.class.getName() + ".startNanos";

    private final ApiAccessLogService apiAccessLogService;

    public ApiAccessLogFilter(ApiAccessLogService apiAccessLogService) {
        this.apiAccessLogService = apiAccessLogService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && "/api/admin/health".equals(uri)) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        request.setAttribute(START_ATTR, System.nanoTime());
        try {
            filterChain.doFilter(request, response);
        } finally {
            Object startObj = request.getAttribute(START_ATTR);
            long startNanos = startObj instanceof Long ? (Long) startObj : System.nanoTime();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            String rid = MDC.get(RequestIdFilter.REQUEST_ID);
            if (rid == null || rid.isBlank()) {
                rid = response.getHeader("X-Request-Id");
            }
            try {
                apiAccessLogService.record(
                        rid != null ? rid : "-",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        durationMs
                );
            } catch (Exception ignored) {
                // Never break the response pipeline for audit persistence failures
            }
        }
    }
}
