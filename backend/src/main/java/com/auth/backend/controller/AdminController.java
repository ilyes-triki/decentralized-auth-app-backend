package com.auth.backend.controller;

import com.auth.backend.dto.ApiAccessLogItem;
import com.auth.backend.dto.AccountBlockAppealItem;
import com.auth.backend.dto.BlockIpRequest;
import com.auth.backend.dto.IpBlocklistItem;
import com.auth.backend.dto.IpRiskEventItem;
import com.auth.backend.dto.LoginHistoryItem;
import com.auth.backend.dto.LoginStatsResponse;
import com.auth.backend.dto.ResolveAccountAppealRequest;
import com.auth.backend.model.AccountBlockAppeal;
import com.auth.backend.model.ApiAccessLog;
import com.auth.backend.model.IpBlocklistEntry;
import com.auth.backend.model.IpRiskEvent;
import com.auth.backend.model.LoginHistory;
import com.auth.backend.repository.ApiAccessLogRepository;
import com.auth.backend.repository.IpBlocklistRepository;
import com.auth.backend.repository.IpRiskEventRepository;
import com.auth.backend.repository.LoginHistoryRepository;
import com.auth.backend.service.AccountAppealService;
import com.auth.backend.service.AccountStatusService;
import com.auth.backend.service.IpBlocklistService;
import com.auth.backend.security.IpAddressNormalizer;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final LoginHistoryRepository loginHistoryRepository;
    private final ApiAccessLogRepository apiAccessLogRepository;
    private final IpRiskEventRepository ipRiskEventRepository;
    private final IpBlocklistRepository ipBlocklistRepository;
    private final IpBlocklistService ipBlocklistService;
    private final AccountAppealService accountAppealService;
    private final AccountStatusService accountStatusService;

    public AdminController(
            LoginHistoryRepository loginHistoryRepository,
            ApiAccessLogRepository apiAccessLogRepository,
            IpRiskEventRepository ipRiskEventRepository,
            IpBlocklistRepository ipBlocklistRepository,
            IpBlocklistService ipBlocklistService,
            AccountAppealService accountAppealService,
            AccountStatusService accountStatusService
    ) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.apiAccessLogRepository = apiAccessLogRepository;
        this.ipRiskEventRepository = ipRiskEventRepository;
        this.ipBlocklistRepository = ipBlocklistRepository;
        this.ipBlocklistService = ipBlocklistService;
        this.accountAppealService = accountAppealService;
        this.accountStatusService = accountStatusService;
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
                        item.getCreatedAt(),
                        item.getClientIp()
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

    @GetMapping("/access-log")
    public List<ApiAccessLogItem> accessLog() {
        return apiAccessLogRepository.findTop80ByOrderByCreatedAtDesc().stream()
                .map(this::toItem)
                .toList();
    }

    @GetMapping("/ip-events")
    public List<IpRiskEventItem> ipEvents(@RequestParam(value = "risk", required = false) String risk) {
        List<IpRiskEvent> rows = ipRiskEventRepository.findTop200ByOrderByCreatedAtDesc();
        if (risk != null && !risk.isBlank()) {
            String r = risk.trim().toUpperCase();
            rows = rows.stream()
                    .filter(e -> r.equalsIgnoreCase(e.getRiskLevel()))
                    .collect(Collectors.toList());
        }
        return rows.stream().map(this::toIpRiskItem).toList();
    }

    @GetMapping("/ip-blocks")
    public List<IpBlocklistItem> ipBlocks() {
        return ipBlocklistRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toIpBlockItem)
                .toList();
    }

    @PostMapping("/ip-blocks")
    public Map<String, String> blockIp(@RequestBody BlockIpRequest body, Authentication authentication) {
        if (body.getIp() == null || body.getIp().isBlank()) {
            throw new IllegalArgumentException("IP is required");
        }
        String ip = body.getIp().trim();
        String reason = body.getReason() != null && !body.getReason().isBlank()
                ? body.getReason().trim()
                : "Blocked by administrator";
        Instant until = body.isPermanent() ? null : Instant.now().plus(30, ChronoUnit.DAYS);
        ipBlocklistService.adminBlock(ip, reason, until, authentication != null ? authentication.getName() : null);
        String storedIp = IpAddressNormalizer.normalize(ip);
        return Map.of("status", "blocked", "ip", storedIp != null ? storedIp : ip);
    }

    @DeleteMapping("/ip-blocks/{ip}")
    public Map<String, String> unblockIp(@PathVariable("ip") String ip) {
        ipBlocklistService.unblock(ip);
        return Map.of("status", "unblocked", "ip", ip);
    }

    @GetMapping("/account-appeals")
    public List<AccountBlockAppealItem> accountAppeals(@RequestParam(value = "status", required = false) String status) {
        return accountAppealService.listAppeals(status).stream().map(this::toAppealItem).toList();
    }

    @PostMapping("/account-appeals/{id}/resolve")
    public AccountBlockAppealItem resolveAppeal(
            @PathVariable("id") long id,
            @RequestBody ResolveAccountAppealRequest body,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "admin";
        AccountBlockAppeal appeal = accountAppealService.resolveAppeal(id, body.isApprove(), body.getAdminNote(), actor);
        return toAppealItem(appeal);
    }

    @PostMapping("/account-blocks/{address}")
    public Map<String, String> blockAccount(@PathVariable("address") String address) {
        accountStatusService.blockAccount(address);
        return Map.of("status", "blocked", "address", address.toLowerCase());
    }

    @DeleteMapping("/account-blocks/{address}")
    public Map<String, String> unblockAccount(@PathVariable("address") String address, Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "admin";
        accountStatusService.unblockAccount(address, actor);
        return Map.of("status", "unblocked", "address", address.toLowerCase());
    }

    private IpRiskEventItem toIpRiskItem(IpRiskEvent row) {
        return new IpRiskEventItem(
                row.getId(),
                row.getIp(),
                row.getRiskLevel(),
                row.getReason(),
                row.getPath(),
                row.getHttpMethod(),
                row.getRequestId(),
                row.getCreatedAt(),
                row.getDetails()
        );
    }

    private IpBlocklistItem toIpBlockItem(IpBlocklistEntry row) {
        return new IpBlocklistItem(
                row.getIp(),
                row.getSource(),
                row.getReason(),
                row.getBlockedUntil(),
                row.getCreatedAt(),
                row.getCreatedBy()
        );
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

    private AccountBlockAppealItem toAppealItem(AccountBlockAppeal row) {
        return new AccountBlockAppealItem(
                row.getId(),
                row.getWalletAddress(),
                row.getEmail(),
                row.getJustification(),
                row.getEvidenceUrl(),
                row.getStatus(),
                row.getAdminNote(),
                row.getCreatedAt(),
                row.getResolvedAt(),
                row.getResolvedBy()
        );
    }
}
