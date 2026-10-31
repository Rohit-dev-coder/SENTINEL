package com.sentinel.gateway.security;

import com.sentinel.gateway.config.TenantConfigClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Exchanges a tenant API key for a short-lived JWT. The key is checked against the admin service
 * on every call (no cache, no fallback), so a deactivated or deleted tenant cannot get a new token.
 */
@RestController
@RequestMapping("/auth")
public class TokenController {

    private final TenantConfigClient tenantConfigClient;
    private final JwtUtil jwtUtil;

    public TokenController(TenantConfigClient tenantConfigClient, JwtUtil jwtUtil) {
        this.tenantConfigClient = tenantConfigClient;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/token")
    public Mono<ResponseEntity<Map<String, String>>> issueToken(
            @RequestHeader(name = "X-Tenant-Api-Key", required = false) String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return tenantConfigClient.lookup(apiKey)
                .filter(config -> config.isActive())
                .map(config -> ResponseEntity.ok(Map.of("token", jwtUtil.generateToken(apiKey), "type", "Bearer")))
                .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build())
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
