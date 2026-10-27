package com.school_guardian.ms_iam.infrastructure.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the RemoteIpValve configuration: X-Forwarded-For is honoured only for
 * requests coming from the internal (Docker / private) proxies, never from a
 * public address that could forge it.
 */
class TrustedProxiesConfigTest {

    private static Properties properties;
    private static Pattern internalProxies;

    @BeforeAll
    static void loadApplicationProperties() throws IOException {
        properties = new Properties();
        try (InputStream in = TrustedProxiesConfigTest.class.getResourceAsStream("/application.properties")) {
            assertNotNull(in, "application.properties not on the classpath");
            properties.load(in);
        }
        internalProxies = Pattern.compile(properties.getProperty("server.tomcat.remoteip.internal-proxies"));
    }

    @Test
    void forwardedHeadersAreHandledByTomcat() {
        assertEquals("native", properties.getProperty("server.forward-headers-strategy"));
        assertEquals("x-forwarded-for", properties.getProperty("server.tomcat.remoteip.remote-ip-header"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"172.18.0.5", "172.31.255.1", "10.0.3.7", "192.168.100.211", "127.0.0.1"})
    void dockerAndPrivateAddressesAreTrustedProxies(String address) {
        assertTrue(internalProxies.matcher(address).matches(), address);
    }

    @ParameterizedTest
    @ValueSource(strings = {"190.24.8.15", "8.8.8.8", "172.32.0.1", "172.15.0.1", "203.0.113.9"})
    void publicAddressesAreNeverTrusted(String address) {
        assertFalse(internalProxies.matcher(address).matches(), address);
    }
}
