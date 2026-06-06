package com.auth.backend.service;

import com.auth.backend.model.IpBlocklistEntry;
import com.auth.backend.repository.IpBlocklistRepository;
import com.auth.backend.security.IpAddressNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class IpBlocklistService {

    public static final String SOURCE_AUTO = "AUTO";
    public static final String SOURCE_ADMIN = "ADMIN";

    private final IpBlocklistRepository ipBlocklistRepository;

    public IpBlocklistService(IpBlocklistRepository ipBlocklistRepository) {
        this.ipBlocklistRepository = ipBlocklistRepository;
    }

    public boolean isBlocked(String ip) {
        for (String candidate : lookupKeys(ip)) {
            if (matchesActiveBlock(candidate)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void autoBlock(String ip, String reason, Duration duration) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        saveBlock(IpAddressNormalizer.normalize(ip), SOURCE_AUTO, reason != null ? reason : "Auto-blocked", Instant.now().plus(duration), null);
    }

    @Transactional
    public void adminBlock(String ip, String reason, Instant blockedUntil, String adminPrincipal) {
        saveBlock(
                IpAddressNormalizer.normalize(ip),
                SOURCE_ADMIN,
                reason != null ? reason : "Blocked by admin",
                blockedUntil,
                adminPrincipal
        );
    }

    @Transactional
    public void unblock(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        for (String candidate : lookupKeys(ip)) {
            ipBlocklistRepository.deleteById(candidate);
        }
    }

    private void saveBlock(String normalizedIp, String source, String reason, Instant blockedUntil, String adminPrincipal) {
        if (normalizedIp == null || normalizedIp.isBlank()) {
            return;
        }
        IpBlocklistEntry entry = ipBlocklistRepository.findById(normalizedIp).orElseGet(IpBlocklistEntry::new);
        entry.setIp(normalizedIp);
        entry.setSource(source);
        entry.setReason(reason);
        entry.setBlockedUntil(blockedUntil);
        entry.setCreatedBy(adminPrincipal);
        ipBlocklistRepository.save(entry);
    }

    private boolean matchesActiveBlock(String key) {
        return ipBlocklistRepository.findById(key)
                .map(entry -> {
                    if (entry.getBlockedUntil() == null) {
                        return true;
                    }
                    return entry.getBlockedUntil().isAfter(Instant.now());
                })
                .orElse(false);
    }

    private Set<String> lookupKeys(String ip) {
        Set<String> keys = new LinkedHashSet<>();
        if (ip == null || ip.isBlank()) {
            return keys;
        }
        String trimmed = ip.trim();
        String normalized = IpAddressNormalizer.normalize(trimmed);
        if (normalized != null) {
            keys.add(normalized);
        }
        keys.add(trimmed);
        if (IpAddressNormalizer.LOOPBACK_CANONICAL.equals(normalized)) {
            keys.add("::1");
            keys.add("0:0:0:0:0:0:0:1");
        }
        return keys;
    }
}
