package com.abchr.platform.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!isPrivate(remote)) {
            return remote;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remote;
        }
        String[] parts = forwarded.split(",");
        String candidate = parts[parts.length - 1].trim();
        return candidate.isEmpty() || candidate.length() > 45 ? remote : candidate;
    }

    static boolean isPrivate(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        try {
            InetAddress ip = InetAddress.getByName(address);
            return ip.isLoopbackAddress() || ip.isSiteLocalAddress() || ip.isLinkLocalAddress()
                    || (ip.getAddress().length == 16 && (ip.getAddress()[0] & 0xFE) == 0xFC);
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
