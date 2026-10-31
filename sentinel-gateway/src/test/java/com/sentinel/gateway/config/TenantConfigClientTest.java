package com.sentinel.gateway.config;

import com.sentinel.common.enums.AlgorithmType;
import com.sentinel.common.enums.Tier;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantConfigClientTest {

    private static final String BODY = """
            {"active":true,"tier":"PREMIUM","defaultMaxRequests":1000,"defaultWindowSeconds":60,
             "rules":[{"endpointPattern":"/api/v1/orders/**","algorithm":"FIXED_WINDOW_COUNTER",
                       "maxRequests":5,"windowSeconds":30,"unknownField":1}]}
            """;

    private HttpServer server;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> tenantKeyHeader = new AtomicReference<>();
    private final AtomicReference<String> adminKeyHeader = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/admin/gateway-config", exchange -> {
            calls.incrementAndGet();
            tenantKeyHeader.set(exchange.getRequestHeaders().getFirst("X-Tenant-Api-Key"));
            adminKeyHeader.set(exchange.getRequestHeaders().getFirst("X-Admin-Key"));
            byte[] body = status.get() == 200 ? BODY.getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private TenantConfigClient client(long ttlSeconds) {
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        return new TenantConfigClient(WebClient.builder(), url, "admin-key", ttlSeconds);
    }

    @Test
    void parsesConfigAndSendsKeys() {
        TenantLimitConfig config = client(30).getConfig("sk_test").block();

        assertNotNull(config);
        assertTrue(config.isActive());
        assertEquals(Tier.PREMIUM, config.getTier());
        assertEquals(1, config.getRules().size());
        assertEquals(AlgorithmType.FIXED_WINDOW_COUNTER, config.getRules().get(0).getAlgorithm());
        assertEquals("sk_test", tenantKeyHeader.get());
        assertEquals("admin-key", adminKeyHeader.get());
    }

    @Test
    void cachesWithinTtl() {
        TenantConfigClient client = client(30);
        client.getConfig("sk_test").block();
        client.getConfig("sk_test").block();

        assertEquals(1, calls.get());
    }

    @Test
    void refetchesWhenTtlIsZero() {
        TenantConfigClient client = client(0);
        client.getConfig("sk_test").block();
        client.getConfig("sk_test").block();

        assertEquals(2, calls.get());
    }

    @Test
    void unknownTenantYieldsEmpty() {
        status.set(404);

        assertNull(client(30).getConfig("sk_missing").block());
    }

    @Test
    void serverErrorFallsBackToFreeTier() {
        status.set(500);

        TenantLimitConfig config = client(30).getConfig("sk_test").block();

        assertNotNull(config);
        assertEquals(Tier.FREE, config.getTier());
        assertEquals(Tier.FREE.getDefaultMaxRequests(), config.getDefaultMaxRequests());
    }

    @Test
    void serverErrorFallsBackToStaleConfig() {
        TenantConfigClient client = client(0);
        client.getConfig("sk_test").block();
        status.set(500);

        TenantLimitConfig config = client.getConfig("sk_test").block();

        assertNotNull(config);
        assertEquals(Tier.PREMIUM, config.getTier());
    }
}
