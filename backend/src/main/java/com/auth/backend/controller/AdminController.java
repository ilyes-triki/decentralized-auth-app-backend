package com.auth.backend.controller;

import com.auth.backend.dto.ApiAccessLogItem;
import com.auth.backend.dto.LoginHistoryItem;
import com.auth.backend.dto.LoginStatsResponse;
import com.auth.backend.model.ApiAccessLog;
import com.auth.backend.model.LoginHistory;
import com.auth.backend.repository.ApiAccessLogRepository;
import com.auth.backend.repository.LoginHistoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final LoginHistoryRepository loginHistoryRepository;
    private final ApiAccessLogRepository apiAccessLogRepository;

    public AdminController(
            LoginHistoryRepository loginHistoryRepository,
            ApiAccessLogRepository apiAccessLogRepository
    ) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.apiAccessLogRepository = apiAccessLogRepository;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "scope", "admin"
        );
    }

    @GetMapping("/login-history")
    public List<LoginHistoryItem> loginHistory(
            @RequestParam(value = "since", required = false) String sinceRaw
    ) {
        Instant since = parseSinceOrNull(sinceRaw);
        List<LoginHistory> rows = since == null
                ? loginHistoryRepository.findTop50ByOrderByCreatedAtDesc()
                : loginHistoryRepository.findTop50ByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(since);
        return rows.stream()
                .map(item -> new LoginHistoryItem(
                        item.getId(),
                        item.getAddress(),
                        item.isSuccessful(),
                        item.getFailureReason(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    @GetMapping("/stats")
    public LoginStatsResponse loginStats(@RequestParam(value = "since", required = false) String sinceRaw) {
        Instant since = parseSinceOrNull(sinceRaw);
        if (since == null) {
            long total = loginHistoryRepository.count();
            long successful = loginHistoryRepository.countBySuccessful(true);
            long failed = loginHistoryRepository.countBySuccessful(false);
            return new LoginStatsResponse(total, successful, failed);
        }
        long total = loginHistoryRepository.countAllSince(since);
        long successful = loginHistoryRepository.countSuccessfulSince(since);
        long failed = loginHistoryRepository.countFailedSince(since);
        return new LoginStatsResponse(total, successful, failed);
    }

    /**
     * Recent HTTP access audit rows (Step 7): method, path, status, duration, optional authenticated principal.
     */
    @GetMapping("/access-log")
    public List<ApiAccessLogItem> accessLog() {
        return apiAccessLogRepository.findTop80ByOrderByCreatedAtDesc().stream()
                .map(this::toItem)
                .toList();
    }

    private ApiAccessLogItem toItem(ApiAccessLog row) {
        return new ApiAccessLogItem(
                row.getId(),
                row.getCreatedAt(),
                row.getRequestId(),
                row.getHttpMethod(),
                row.getPath(),
                row.getStatusCode(),
                row.getDurationMs(),
                row.getPrincipal()
        );
    }

    private static Instant parseSinceOrNull(String sinceRaw) {
        if (sinceRaw == null || sinceRaw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(sinceRaw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid since parameter; use ISO-8601 UTC instant (e.g. 2025-04-01T00:00:00Z).");
        }
    }
}
