package demo;

import com.sentinel.limiter.RateLimitResult;
import com.sentinel.limiter.RateLimiter;
import com.sentinel.limiter.RateLimiterFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@RestController
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of("message", "hello");
    }

    @GetMapping("/api/orders/1")
    public Map<String, String> order() {
        return Map.of("order", "1");
    }

    /**
     * No Redis server is available where this demo runs, so replace the starter's Redis-backed limiters
     * with a simple in-memory fixed window. A real application would skip this bean and use Redis.
     */
    @Bean
    RateLimiterFactory rateLimiterFactory() {
        RateLimiter inMemory = new RateLimiter() {
            private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

            @Override
            public RateLimitResult allowRequest(String key, int maxRequests, int windowSeconds) {
                long now = System.currentTimeMillis() / 1000;
                long windowStart = now - (now % windowSeconds);
                int used = counts.computeIfAbsent(key + ":" + windowStart, k -> new AtomicInteger()).incrementAndGet();
                return new RateLimitResult(used <= maxRequests, Math.max(0, maxRequests - used), windowStart + windowSeconds);
            }
        };
        return new RateLimiterFactory(inMemory, inMemory, inMemory, inMemory);
    }
}
