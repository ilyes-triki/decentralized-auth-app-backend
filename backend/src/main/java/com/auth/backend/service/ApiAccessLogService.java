package com.auth.backend.service;

import com.auth.backend.model.ApiAccessLog;
import com.auth.backend.repository.ApiAccessLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ApiAccessLogService {

    private static final Logger STRUCTURED = LoggerFactory.getLogger("AUDIT_HTTP");

    private final ApiAccessLogRepository repository;

    public ApiAccessLogService(ApiAccessLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(
            String requestId,
            String method,
            String path,
            int statusCode,
            long durationMs
    ) {
        String principal = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            principal = auth.getName();
            if (principal != null && principal.length() > 255) {
                principal = principal.substring(0, 255);
            }
        }

        ApiAccessLog row = new ApiAccessLog();
        row.setCreatedAt(Instant.now());
        row.setRequestId(requestId != null && !requestId.isBlank() ? requestId : "-");
        row.setHttpMethod(method != null ? method : "?");
        row.setPath(path != null && path.length() > 512 ? path.substring(0, 512) : path);
        row.setStatusCode(statusCode);
        row.setDurationMs(durationMs);
        row.setPrincipal(principal);

        repository.save(row);

        STRUCTURED.info(
                "event=api_access requestId={} method={} path={} status={} durationMs={} principal={}",
                row.getRequestId(),
                row.getHttpMethod(),
                row.getPath(),
                statusCode,
                durationMs,
                principal != null ? principal : "-"
        );
    }
}
