package com.auth.backend.service;

import com.auth.backend.model.IpBlocklistEntry;
import com.auth.backend.repository.IpBlocklistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class IpBlocklistService {

    public static final String SOURCE_AUTO = "AUTO";
    public static final String SOURCE_ADMIN = "ADMIN";

    private final IpBlocklistRepository ipBlocklistRepository;

    public IpBlocklistService(IpBlocklistRepository ipBlocklistRepository) {
        this.ipBlocklistRepository = ipBlocklistRepository;
    }

    public boolean isBlocked(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return ipBlocklistRepository.findById(ip.trim())
                .map(entry -> {
                    if (entry.getBlockedUntil() == null) {
                        return true;
                    }
                    return entry.getBlockedUntil().isAfter(Instant.now());
                })
                .orElse(false);
    }

    @Transactional
    public void autoBlock(String ip, String reason, Duration duration) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String normalized = ip.trim();
        IpBlocklistEntry entry = ipBlocklistRepository.findById(normalized).orElseGet(IpBlocklistEntry::new);
        entry.setIp(normalized);
        entry.setSource(SOURCE_AUTO);
        entry.setReason(reason != null ? reason : "Auto-blocked");
        entry.setBlockedUntil(Instant.now().plus(duration));
        entry.setCreatedBy(null);
        ipBlocklistRepository.save(entry);
    }

    @Transactional
    public void adminBlock(String ip, String reason, Instant blockedUntil, String adminPrincipal) {
        String normalized = ip.trim();
        IpBlocklistEntry entry = ipBlocklistRepository.findById(normalized).orElseGet(IpBlocklistEntry::new);
        entry.setIp(normalized);
        entry.setSource(SOURCE_ADMIN);
        entry.setReason(reason != null ? reason : "Blocked by admin");
        entry.setBlockedUntil(blockedUntil);
        entry.setCreatedBy(adminPrincipal);
        ipBlocklistRepository.save(entry);
    }

    @Transactional
    public void unblock(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        ipBlocklistRepository.deleteById(ip.trim());
    }
}
