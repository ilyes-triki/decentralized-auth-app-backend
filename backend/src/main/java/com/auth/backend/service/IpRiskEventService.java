package com.auth.backend.service;

import com.auth.backend.model.IpRiskEvent;
import com.auth.backend.repository.IpRiskEventRepository;
import com.auth.backend.security.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IpRiskEventService {

    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";

    private final IpRiskEventRepository ipRiskEventRepository;

    public IpRiskEventService(IpRiskEventRepository ipRiskEventRepository) {
        this.ipRiskEventRepository = ipRiskEventRepository;
    }

    @Transactional
    public void record(String ip, String riskLevel, String reason, HttpServletRequest request, String details) {
        IpRiskEvent row = new IpRiskEvent();
        row.setIp(ip != null ? ip : "unknown");
        row.setRiskLevel(riskLevel);
        row.setReason(reason);
        row.setPath(request.getRequestURI());
        row.setHttpMethod(request.getMethod());
        row.setRequestId(MDC.get(RequestIdFilter.REQUEST_ID));
        if (details != null && details.length() > 1900) {
            row.setDetails(details.substring(0, 1900) + "…");
        } else {
            row.setDetails(details);
        }
        ipRiskEventRepository.save(row);
    }

    @Transactional
    public void recordSimple(String ip, String riskLevel, String reason, String path, String method, String details) {
        IpRiskEvent row = new IpRiskEvent();
        row.setIp(ip != null ? ip : "unknown");
        row.setRiskLevel(riskLevel);
        row.setReason(reason);
        row.setPath(path != null ? path : "");
        row.setHttpMethod(method != null ? method : "");
        row.setRequestId(MDC.get(RequestIdFilter.REQUEST_ID));
        if (details != null && details.length() > 1900) {
            row.setDetails(details.substring(0, 1900) + "…");
        } else {
            row.setDetails(details);
        }
        ipRiskEventRepository.save(row);
    }
}
