package com.auth.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final int maxRequestsPerWindow;
    private final long windowSeconds;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            @Value("${app.security.rate-limit.max-requests:20}") int maxRequestsPerWindow,
            @Value("${app.security.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean rateLimited = "/api/auth/nonce".equals(path)
                || "/api/auth/login".equals(path)
                || "/api/auth/email/start".equals(path)
                || "/api/auth/email/verify".equals(path);
        if (!rateLimited) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr() + ":" + path;
        Instant now = Instant.now();
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart.plusSeconds(windowSeconds).isBefore(now)) {
                return new WindowCounter(now, 1);
            }

            return new WindowCounter(existing.windowStart, existing.count + 1);
        });

        if (counter.count > maxRequestsPerWindow) {
            throw new RateLimitExceededException("Too many authentication attempts. Please retry shortly.");
        }

        filterChain.doFilter(request, response);
    }

    private record WindowCounter(Instant windowStart, int count) {}
}
