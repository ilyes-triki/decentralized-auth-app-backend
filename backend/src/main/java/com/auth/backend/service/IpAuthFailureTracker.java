package com.auth.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpAuthFailureTracker {

    private final ConcurrentHashMap<String, Deque<Instant>> failuresByIp = new ConcurrentHashMap<>();
    private final int windowSeconds;
    private final int failureThreshold;
    private final IpBlocklistService ipBlocklistService;
    private final IpRiskEventService ipRiskEventService;

    public IpAuthFailureTracker(
            @Value("${app.security.ip.auth-failure-window-seconds:300}") int windowSeconds,
            @Value("${app.security.ip.auth-failure-threshold:20}") int failureThreshold,
            IpBlocklistService ipBlocklistService,
            IpRiskEventService ipRiskEventService
    ) {
        this.windowSeconds = windowSeconds;
        this.failureThreshold = failureThreshold;
        this.ipBlocklistService = ipBlocklistService;
        this.ipRiskEventService = ipRiskEventService;
    }

    /**
     * Records a failed wallet login attempt; may auto-block the IP after repeated failures.
     */
    public void recordFailure(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String normalized = ip.trim();
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        Deque<Instant> dq = failuresByIp.computeIfAbsent(normalized, k -> new ArrayDeque<>());
        synchronized (dq) {
            while (!dq.isEmpty() && dq.peekFirst().isBefore(cutoff)) {
                dq.pollFirst();
            }
            dq.addLast(Instant.now());
            if (dq.size() >= failureThreshold) {
                ipRiskEventService.recordSimple(
                        normalized,
                        IpRiskEventService.RISK_HIGH,
                        "Repeated failed authentication attempts",
                        "/api/auth/login",
                        "POST",
                        "failuresInWindow=" + dq.size()
                );
                ipBlocklistService.autoBlock(
                        normalized,
                        "Too many failed authentication attempts",
                        Duration.ofMinutes(30)
                );
            }
        }
    }
}
