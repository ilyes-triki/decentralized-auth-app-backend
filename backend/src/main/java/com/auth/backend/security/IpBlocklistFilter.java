package com.auth.backend.security;

import com.auth.backend.dto.ApiErrorResponse;
import com.auth.backend.service.IpBlocklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class IpBlocklistFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IpBlocklistFilter.class);

    private final IpBlocklistService ipBlocklistService;
    private final boolean trustForwardedFor;
    private final ObjectMapper objectMapper;

    public IpBlocklistFilter(
            IpBlocklistService ipBlocklistService,
            @Value("${app.security.ip.trust-x-forwarded-for:false}") boolean trustForwardedFor,
            ObjectMapper objectMapper
    ) {
        this.ipBlocklistService = ipBlocklistService;
        this.trustForwardedFor = trustForwardedFor;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = ClientIpResolver.resolve(request, trustForwardedFor);
        if (ipBlocklistService.isBlocked(ip)) {
            logger.info("Rejecting blocked IP {} on {}", ip, path);
            writeBlocked(response, "This IP address is blocked from accessing the API.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeBlocked(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiErrorResponse(message, HttpServletResponse.SC_FORBIDDEN, "IP_BLOCKED")
        );
    }
}
