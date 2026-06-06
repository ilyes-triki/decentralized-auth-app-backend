package com.auth.backend.service;

import com.auth.backend.repository.IpBlocklistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class IpBlocklistServiceTest {

    @Autowired
    private IpBlocklistService ipBlocklistService;

    @Autowired
    private IpBlocklistRepository ipBlocklistRepository;

    @Test
    void permanentBlockPersists() {
        ipBlocklistService.adminBlock("203.0.113.60", "test permanent", null, "admin");

        assertTrue(ipBlocklistService.isBlocked("203.0.113.60"));
        assertTrue(ipBlocklistRepository.findById("203.0.113.60").isPresent());
    }

    @Test
    void expiredBlockDoesNotBlock() {
        ipBlocklistService.adminBlock(
                "203.0.113.61",
                "expired",
                Instant.now().minus(1, ChronoUnit.HOURS),
                "admin"
        );

        assertFalse(ipBlocklistService.isBlocked("203.0.113.61"));
    }

    @Test
    void unblockRemovesBlock() {
        ipBlocklistService.adminBlock("203.0.113.62", "temporary", null, "admin");
        assertTrue(ipBlocklistService.isBlocked("203.0.113.62"));

        ipBlocklistService.unblock("203.0.113.62");
        assertFalse(ipBlocklistService.isBlocked("203.0.113.62"));
    }

    @Test
    void loopbackBlockMatchesIpv6RemoteRepresentation() {
        ipBlocklistService.adminBlock("127.0.0.1", "local dev", null, "admin");

        assertTrue(ipBlocklistService.isBlocked("0:0:0:0:0:0:0:1"));
        assertTrue(ipBlocklistService.isBlocked("::1"));
    }
}
