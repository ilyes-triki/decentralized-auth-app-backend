package com.auth.backend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IpAddressNormalizerTest {

    @Test
    void normalizesLoopbackVariantsToCanonicalIpv4() {
        assertEquals("127.0.0.1", IpAddressNormalizer.normalize("127.0.0.1"));
        assertEquals("127.0.0.1", IpAddressNormalizer.normalize("::1"));
        assertEquals("127.0.0.1", IpAddressNormalizer.normalize("0:0:0:0:0:0:0:1"));
    }

    @Test
    void normalizesIpv4MappedIpv6() {
        assertEquals("203.0.113.50", IpAddressNormalizer.normalize("::ffff:203.0.113.50"));
    }

    @Test
    void blankInputReturnsNull() {
        assertNull(IpAddressNormalizer.normalize("  "));
        assertNull(IpAddressNormalizer.normalize(null));
    }
}
