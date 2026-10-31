package com.sentinel.starter;

import com.sentinel.limiter.RateLimiterFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SentinelAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SentinelAutoConfiguration.class))
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    @Test
    void registersFilterAndLimitersByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(SentinelRateLimitFilter.class);
            assertThat(context).hasSingleBean(RateLimiterFactory.class);
            assertThat(context).hasSingleBean(LimitResolver.class);
        });
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("sentinel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SentinelRateLimitFilter.class));
    }

    @Test
    void bindsRulesFromProperties() {
        runner.withPropertyValues(
                "sentinel.default-limit.max-requests=20",
                "sentinel.rules[0].path-pattern=/api/orders/**",
                "sentinel.rules[0].algorithm=FIXED_WINDOW_COUNTER",
                "sentinel.rules[0].max-requests=5",
                "sentinel.rules[0].window-seconds=30"
        ).run(context -> {
            SentinelProperties properties = context.getBean(SentinelProperties.class);
            assertThat(properties.getDefaultLimit().getMaxRequests()).isEqualTo(20);
            assertThat(properties.getRules()).hasSize(1);
            assertThat(properties.getRules().get(0).getPathPattern()).isEqualTo("/api/orders/**");
            assertThat(properties.getRules().get(0).getMaxRequests()).isEqualTo(5);
        });
    }

    @Test
    void failsFastOnInvalidConfiguration() {
        runner.withPropertyValues("sentinel.default-limit.max-requests=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsFastOnRulePatternWithoutLeadingSlash() {
        runner.withPropertyValues("sentinel.rules[0].path-pattern=api/orders")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void enablesConnectedModeWhenAdminUrlIsSet() {
        runner.withPropertyValues("sentinel.admin.url=http://localhost:8081", "sentinel.admin.api-key=k")
                .run(context -> {
                    assertThat(context).hasSingleBean(SentinelRateLimitFilter.class);
                    assertThat(context.getBean(SentinelProperties.class).getAdmin().isEnabled()).isTrue();
                });
    }
}
