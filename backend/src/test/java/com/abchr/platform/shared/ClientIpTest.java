package com.abchr.platform.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.abchr.platform.shared.web.ClientIp;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpTest {

    @Test
    void usesForwardedAddressFromPrivateProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.9");
        assertEquals("203.0.113.9", ClientIp.of(request));
    }

    @Test
    void takesTheEntryAddedByTheProxyNotTheClientSuppliedOne() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.9");
        assertEquals("203.0.113.9", ClientIp.of(request));
    }

    @Test
    void ignoresForwardedHeaderFromPublicAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        assertEquals("198.51.100.7", ClientIp.of(request));
    }

    @Test
    void fallsBackToRemoteAddressWithoutHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        assertEquals("127.0.0.1", ClientIp.of(request));
    }
}
