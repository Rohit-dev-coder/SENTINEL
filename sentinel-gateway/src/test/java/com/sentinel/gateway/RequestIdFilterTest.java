package com.sentinel.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private String run(MockServerHttpRequest request, AtomicReference<String> seenDownstream) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, (ServerWebExchange e) -> {
            seenDownstream.set(e.getRequest().getHeaders().getFirst(RequestIdFilter.HEADER));
            return Mono.empty();
        }).block();
        return exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER);
    }

    @Test
    void generatesIdWhenMissing() {
        AtomicReference<String> downstream = new AtomicReference<>();
        String responseId = run(MockServerHttpRequest.get("/x").build(), downstream);

        assertNotNull(responseId);
        assertEquals(responseId, downstream.get());
    }

    @Test
    void keepsSafeClientId() {
        AtomicReference<String> downstream = new AtomicReference<>();
        String responseId = run(MockServerHttpRequest.get("/x").header(RequestIdFilter.HEADER, "client-id-12345").build(), downstream);

        assertEquals("client-id-12345", responseId);
        assertEquals("client-id-12345", downstream.get());
    }

    @Test
    void replacesUnsafeClientId() {
        AtomicReference<String> downstream = new AtomicReference<>();
        String responseId = run(MockServerHttpRequest.get("/x").header(RequestIdFilter.HEADER, "bad id\twith spaces").build(), downstream);

        assertNotEquals("bad id\twith spaces", responseId);
        assertEquals(responseId, downstream.get());
    }
}
