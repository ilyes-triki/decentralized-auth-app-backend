package com.auth.backend.security;

import com.auth.backend.dto.ApiErrorResponse;
import com.auth.backend.http.CachedBodyHttpServletRequest;
import com.auth.backend.service.IpBlocklistService;
import com.auth.backend.service.IpRiskEventService;
import com.auth.backend.service.IpThreatAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class IpRiskAnalysisFilter extends OncePerRequestFilter {

    private final IpThreatAnalyzer ipThreatAnalyzer;
    private final IpRiskEventService ipRiskEventService;
    private final IpBlocklistService ipBlocklistService;
    private final boolean trustForwardedFor;
    private final long autoBlockMinutes;
    private final ObjectMapper objectMapper;

    public IpRiskAnalysisFilter(
            IpThreatAnalyzer ipThreatAnalyzer,
            IpRiskEventService ipRiskEventService,
            IpBlocklistService ipBlocklistService,
            @Value("${app.security.ip.trust-x-forwarded-for:false}") boolean trustForwardedFor,
            @Value("${app.security.ip.auto-block-duration-minutes:30}") long autoBlockMinutes,
            ObjectMapper objectMapper
    ) {
        this.ipThreatAnalyzer = ipThreatAnalyzer;
        this.ipRiskEventService = ipRiskEventService;
        this.ipBlocklistService = ipBlocklistService;
        this.trustForwardedFor = trustForwardedFor;
        this.autoBlockMinutes = autoBlockMinutes;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = ClientIpResolver.resolve(request, trustForwardedFor);
        String query = request.getQueryString() != null ? request.getQueryString() : "";
        String queryRisk = ipThreatAnalyzer.analyzeText(query);

        HttpServletRequest toUse = request;
        String bodyRisk = IpRiskEventService.RISK_LOW;
        String bodySnippet = "";
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
            if (body.length > 65536) {
                ipRiskEventService.record(ip, IpRiskEventService.RISK_HIGH, "Oversized auth payload", request, "size=" + body.length);
                ipBlocklistService.autoBlock(ip, "Oversized auth payload", Duration.ofMinutes(autoBlockMinutes));
                writeBlocked(response, "Request rejected for security reasons.");
                return;
            }
            bodyRisk = ipThreatAnalyzer.analyzeBytes(body);
            bodySnippet = snippet(new String(body, StandardCharsets.UTF_8));
            toUse = new CachedBodyHttpServletRequest(request, body);
        }

        String risk = worst(queryRisk, bodyRisk);
        if (IpRiskEventService.RISK_HIGH.equals(risk)) {
            ipRiskEventService.record(
                    ip,
                    IpRiskEventService.RISK_HIGH,
                    "Suspicious payload or query pattern",
                    request,
                    trim(query + " | " + bodySnippet)
            );
            ipBlocklistService.autoBlock(ip, "High-risk traffic pattern", Duration.ofMinutes(autoBlockMinutes));
            writeBlocked(response, "Request blocked due to suspicious content.");
            return;
        }
        if (IpRiskEventService.RISK_MEDIUM.equals(risk)) {
            ipRiskEventService.record(ip, IpRiskEventService.RISK_MEDIUM, "Suspicious pattern (review)", request, trim(query + " | " + bodySnippet));
        }

        filterChain.doFilter(toUse, response);
    }

    private void writeBlocked(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiErrorResponse(message, HttpServletResponse.SC_FORBIDDEN, "IP_BLOCKED")
        );
    }

    private static String worst(String a, String b) {
        if (IpRiskEventService.RISK_HIGH.equals(a) || IpRiskEventService.RISK_HIGH.equals(b)) {
            return IpRiskEventService.RISK_HIGH;
        }
        if (IpRiskEventService.RISK_MEDIUM.equals(a) || IpRiskEventService.RISK_MEDIUM.equals(b)) {
            return IpRiskEventService.RISK_MEDIUM;
        }
        return IpRiskEventService.RISK_LOW;
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) + "…" : s;
    }

    private static String snippet(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.length() > 240 ? s.substring(0, 240) + "…" : s;
    }
}
