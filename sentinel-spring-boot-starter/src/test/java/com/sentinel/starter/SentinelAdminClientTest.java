package com.sentinel.starter;

import com.sentinel.common.enums.AlgorithmType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentinelAdminClientTest {

    private static final String BODY = """
            {"active":true,"tier":"PREMIUM","defaultMaxRequests":1000,"defaultWindowSeconds":60,
             "rules":[{"endpointPattern":"/api/orders/**","algorithm":"FIXED_WINDOW_COUNTER",
                       "maxRequests":5,"windowSeconds":30,"somethingNew":true}]}
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

    private SentinelAdminClient client(Duration ttl) {
        SentinelProperties.Admin admin = new SentinelProperties.Admin();
        admin.setUrl("http://127.0.0.1:" + server.getAddress().getPort());
        admin.setApiKey("admin-key");
        admin.setCacheTtl(ttl);
        return new SentinelAdminClient(admin);
    }

    @Test
    void parsesConfigAndSendsBothKeys() {
        Optional<AdminTenantConfig> config = client(Duration.ofSeconds(30)).lookup("sk_test");

        assertTrue(config.isPresent());
        assertEquals("PREMIUM", config.get().getTier());
        assertEquals(1, config.get().getRules().size());
        assertEquals(AlgorithmType.FIXED_WINDOW_COUNTER, config.get().getRules().get(0).getAlgorithm());
        assertEquals("sk_test", tenantKeyHeader.get());
        assertEquals("admin-key", adminKeyHeader.get());
    }

    @Test
    void cachesWithinTtl() {
        SentinelAdminClient client = client(Duration.ofSeconds(30));
        client.lookup("sk_test");
        client.lookup("sk_test");

        assertEquals(1, calls.get());
    }

    @Test
    void refetchesWhenTtlIsZero() {
        SentinelAdminClient client = client(Duration.ZERO);
        client.lookup("sk_test");
        client.lookup("sk_test");

        assertEquals(2, calls.get());
    }

    @Test
    void unknownTenantIsEmpty() {
        status.set(404);

        assertTrue(client(Duration.ofSeconds(30)).lookup("sk_missing").isEmpty());
    }

    @Test
    void serverErrorFallsBackToFreeTier() {
        status.set(500);

        AdminTenantConfig config = client(Duration.ofSeconds(30)).lookup("sk_test").orElseThrow();

        assertEquals("FREE", config.getTier());
        assertTrue(config.isActive());
    }

    @Test
    void serverErrorFallsBackToStaleConfig() {
        SentinelAdminClient client = client(Duration.ZERO);
        client.lookup("sk_test");
        status.set(500);

        assertEquals("PREMIUM", client.lookup("sk_test").orElseThrow().getTier());
    }
}
