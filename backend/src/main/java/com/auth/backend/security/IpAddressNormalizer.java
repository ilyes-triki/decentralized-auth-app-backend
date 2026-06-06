package com.auth.backend.security;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Canonical IP strings for blocklist storage and lookup.
 * Ensures loopback and IPv4-mapped IPv6 forms match the same block entry.
 */
public final class IpAddressNormalizer {

    public static final String LOOPBACK_CANONICAL = "127.0.0.1";

    private IpAddressNormalizer() {}

    public static String normalize(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String trimmed = ip.trim();
        try {
            InetAddress address = InetAddress.getByName(trimmed);
            if (address.isLoopbackAddress()) {
                return LOOPBACK_CANONICAL;
            }
            if (address instanceof Inet6Address inet6) {
                byte[] bytes = inet6.getAddress();
                if (isIpv4Mapped(bytes)) {
                    return String.format(
                            "%d.%d.%d.%d",
                            bytes[12] & 0xff,
                            bytes[13] & 0xff,
                            bytes[14] & 0xff,
                            bytes[15] & 0xff
                    );
                }
                return inet6.getHostAddress();
            }
            if (address instanceof Inet4Address inet4) {
                return inet4.getHostAddress();
            }
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            return trimmed;
        }
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        return bytes.length == 16
                && bytes[0] == 0
                && bytes[1] == 0
                && bytes[2] == 0
                && bytes[3] == 0
                && bytes[4] == 0
                && bytes[5] == 0
                && bytes[6] == 0
                && bytes[7] == 0
                && bytes[8] == 0
                && bytes[9] == 0
                && bytes[10] == (byte) 0xff
                && bytes[11] == (byte) 0xff;
    }
}
