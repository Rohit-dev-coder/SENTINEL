package com.sentinel.starter;

import com.sentinel.limiter.RateLimitResult;
import com.sentinel.limiter.RateLimiterFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Limits requests per client and endpoint.
 * <p>
 * Standalone mode takes limits from {@code sentinel.default-limit} and {@code sentinel.rules}.
 * Connected mode ({@code sentinel.admin.url} set) treats the client id header as a tenant API key and asks
 * the Sentinel admin service for that tenant's tier and rules.
 */
public class SentinelRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SentinelRateLimitFilter.class);

    private final SentinelProperties properties;
    private final RateLimiterFactory limiters;
    private final LimitResolver resolver;
    private final SentinelAdminClient adminClient; // null in standalone mode
    private final AntPathMatcher matcher = new AntPathMatcher();

    public SentinelRateLimitFilter(SentinelProperties properties, RateLimiterFactory limiters,
                                   LimitResolver resolver, SentinelAdminClient adminClient) {
        this.properties = properties;
        this.limiters = limiters;
        this.resolver = resolver;
        this.adminClient = adminClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true; // CORS preflight carries no credentials and must not be throttled
        }
        String path = pathWithinApplication(request);
        return properties.getExcludePaths().stream().anyMatch(pattern -> matcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String clientId = clientId(request);
        if (clientId == null) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "missing_client_id");
            return;
        }

        String path = pathWithinApplication(request);
        EffectiveLimit limit;
        if (adminClient != null) {
            Optional<AdminTenantConfig> tenant = adminClient.lookup(clientId);
            if (tenant.isEmpty() || !tenant.get().isActive()) {
                reject(response, HttpServletResponse.SC_FORBIDDEN, "unknown_or_inactive_tenant");
                return;
            }
            limit = resolver.resolve(adminRules(tenant.get()), adminDefault(tenant.get()), path);
        } else {
            limit = resolver.resolve(propertyRules(), propertyDefault(), path);
        }

        RateLimitResult result;
        try {
            result = limiters.getRateLimiter(limit.algorithm())
                    .allowRequest(clientId + ":" + request.getMethod() + ":" + path, limit.maxRequests(), limit.windowSeconds());
        } catch (RuntimeException e) {
            if (properties.isFailOpen()) {
                log.error("Rate limiter unavailable, allowing request: {}", e.toString());
                chain.doFilter(request, response);
            } else {
                log.error("Rate limiter unavailable, rejecting request: {}", e.toString());
                reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "rate_limiter_unavailable");
            }
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit.maxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.isAllowed() ? result.getRemaining() : 0));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetTime()));

        if (!result.isAllowed()) {
            long retryAfter = Math.max(1, result.getResetTime() - System.currentTimeMillis() / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            reject(response, 429, "rate_limit_exceeded");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientId(HttpServletRequest request) {
        String header = request.getHeader(properties.getClientIdHeader());
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        if (adminClient != null) {
            return null; // connected mode always needs a tenant key
        }
        return properties.getIdentityFallback() == SentinelProperties.IdentityFallback.CLIENT_IP
                ? "ip-" + request.getRemoteAddr()
                : null;
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context != null && !context.isEmpty() && uri.startsWith(context) ? uri.substring(context.length()) : uri;
    }

    private List<LimitResolver.RuleSpec> propertyRules() {
        return properties.getRules().stream()
                .map(r -> new LimitResolver.RuleSpec(r.getPathPattern(),
                        new EffectiveLimit(r.getAlgorithm(), r.getMaxRequests(), r.getWindowSeconds())))
                .toList();
    }

    private EffectiveLimit propertyDefault() {
        SentinelProperties.Limit d = properties.getDefaultLimit();
        return new EffectiveLimit(d.getAlgorithm(), d.getMaxRequests(), d.getWindowSeconds());
    }

    private static List<LimitResolver.RuleSpec> adminRules(AdminTenantConfig tenant) {
        return tenant.getRules().stream()
                .map(r -> new LimitResolver.RuleSpec(r.getEndpointPattern(),
                        new EffectiveLimit(r.getAlgorithm(), r.getMaxRequests(), r.getWindowSeconds())))
                .toList();
    }

    private static EffectiveLimit adminDefault(AdminTenantConfig tenant) {
        return new EffectiveLimit(com.sentinel.common.enums.AlgorithmType.TOKEN_BUCKET,
                tenant.getDefaultMaxRequests(), tenant.getDefaultWindowSeconds());
    }

    private static void reject(HttpServletResponse response, int status, String error) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + error + "\"}");
    }
}
