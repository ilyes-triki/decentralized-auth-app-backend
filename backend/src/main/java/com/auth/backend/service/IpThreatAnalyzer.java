package com.auth.backend.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class IpThreatAnalyzer {

    private static final Pattern HIGH_SQLI = Pattern.compile(
            "(?i)(union\\s+select|or\\s+1\\s*=\\s*1|;\\s*--|benchmark\\s*\\(|sleep\\s*\\(|exec\\s*\\(|information_schema)"
    );
    private static final Pattern HIGH_XSS = Pattern.compile(
            "(?i)(<script|javascript:|onerror\\s*=|onload\\s*=|eval\\s*\\()"
    );
    private static final Pattern HIGH_PATH = Pattern.compile("(\\.\\./|%2e%2e%2f|%2e%2e\\\\)");
    private static final Pattern HIGH_TEMPLATE = Pattern.compile("(\\$\\{\\{|<%|\\$\\{)");

    public String analyzeText(String combined) {
        if (combined == null || combined.isBlank()) {
            return IpRiskEventService.RISK_LOW;
        }
        String s = combined;
        if (HIGH_PATH.matcher(s).find()) {
            return IpRiskEventService.RISK_HIGH;
        }
        if (HIGH_XSS.matcher(s).find()) {
            return IpRiskEventService.RISK_HIGH;
        }
        if (HIGH_SQLI.matcher(s).find()) {
            return IpRiskEventService.RISK_HIGH;
        }
        if (HIGH_TEMPLATE.matcher(s).find()) {
            return IpRiskEventService.RISK_MEDIUM;
        }
        return IpRiskEventService.RISK_LOW;
    }

    public String analyzeBytes(byte[] body) {
        if (body == null || body.length == 0) {
            return IpRiskEventService.RISK_LOW;
        }
        String s = new String(body, StandardCharsets.UTF_8);
        return analyzeText(s);
    }
}
