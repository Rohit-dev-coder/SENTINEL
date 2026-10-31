# SENTINEL Architecture Documentation

## System Overview

SENTINEL is a distributed API Gateway system that provides rate limiting, authentication, and routing capabilities for microservices. The system is designed to be highly scalable, extensible, and production-ready.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Gateway Layer (Spring Cloud Gateway)             │
├─────────────────────────────────────────────────────────────────┤
│  • JWT Authentication Filter                                        │
│  • Rate Limiting Filter (4 algorithms)                            │
│  • Circuit Breaker (Resilience4j)                                 │
│  • Request Routing                                                │
│  • Prometheus Metrics                                            │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Downstream Services Layer                      │
├─────────────────────────────────────────────────────────────────┤
│  • Sample Service 1 (Users, Products, Orders)                      │
│  • Sample Service 2 (Analytics, Reports, Dashboard)                │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Configuration & Data Layer                     │
├─────────────────────────────────────────────────────────────────┤
│  • Admin Service (REST API)                                      │
│  • Redis (Rate limiting state, Cache)                             │
│  • PostgreSQL (Tenant and rule configuration)                     │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Monitoring & Observability                     │
├─────────────────────────────────────────────────────────────────┤
│  • Prometheus (Metrics collection)                                 │
│  • Grafana (Visualization)                                        │
└─────────────────────────────────────────────────────────────────┘
```

## Module Architecture

### sentinel-common
**Purpose**: Shared entities, DTOs, and enums

**Key Components**:
- `AlgorithmType` enum: Defines rate limiting algorithms
- `Tenant`, `RateLimitRule`, `RateLimitEvent` entities
- `TenantRequest`, `TenantResponse`, `RuleRequest`, `RuleResponse` DTOs

### sentinel-gateway
**Purpose**: API Gateway with authentication, rate limiting, and routing

**Key Components**:
- `JwtUtil`: JWT token generation and validation
- `JwtAuthenticationFilter`: Gateway filter for JWT authentication
- `RateLimiter` interface: Abstraction for rate limiting algorithms
- `TokenBucketRateLimiter`: Token bucket algorithm implementation
- `SlidingWindowLogRateLimiter`: Sliding window log algorithm
- `FixedWindowCounterRateLimiter`: Fixed window counter algorithm
- `SlidingWindowCounterRateLimiter`: Sliding window counter algorithm
- `RateLimiterFactory`: Factory for selecting algorithms
- `RateLimitFilter`: Gateway filter for rate limiting
- `CircuitBreakerConfig`: Resilience4j configuration

### sentinel-admin
**Purpose**: Configuration service for tenants and rate limiting rules

**Key Components**:
- `TenantService`: Business logic for tenant management
- `RateLimitRuleService`: Business logic for rate limit rules
- `TenantController`: REST endpoints for tenant operations
- `RuleController`: REST endpoints for rule operations
- `RedisConfigCache`: Redis caching for configuration
- `TenantRepository`, `RateLimitRuleRepository`: JPA repositories

### infrastructure
**Purpose**: Docker Compose setup and infrastructure tests

**Key Components**:
- `docker-compose.yml`: Infrastructure orchestration
- `DockerComposeConfigurationTest`: Infrastructure validation
- `CIConfigurationTest`: CI/CD configuration validation

### downstream-services
**Purpose**: Sample services for testing gateway routing

**Key Components**:
- `sample-service-1`: Users, Products, Orders endpoints
- `sample-service-2`: Analytics, Reports, Dashboard endpoints

### load-tests
**Purpose**: Performance testing with Gatling

**Key Components**:
- `SentinelLoadTest.scala`: Gatling load test scenarios
- `LoadTestSetupTest.java`: Load test validation

## Rate Limiting Architecture

### Algorithm Selection

The system supports 4 rate limiting algorithms, each with different trade-offs:

1. **Token Bucket**
   - **Pros**: Smooth rate limiting, burst capability
   - **Cons**: More complex implementation
   - **Use Case**: APIs that allow occasional bursts

2. **Sliding Window Log**
   - **Pros**: Most precise, no boundary effects
   - **Cons**: Higher memory usage
   - **Use Case**: Critical APIs requiring precise limiting

3. **Fixed Window Counter**
   - **Pros**: Simple implementation, low memory
   - **Cons**: Boundary effects (burst at window start)
   - **Use Case**: Non-critical APIs, simple use cases

4. **Sliding Window Counter**
   - **Pros**: Balance between precision and memory
   - **Cons**: More complex than fixed window
   - **Use Case**: General-purpose rate limiting

### Redis Lua Scripts

All rate limiting algorithms use Redis Lua scripts for atomic operations:

```lua
-- Token Bucket Lua Script Example
local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local window_seconds = tonumber(ARGV[2])
local current_time = tonumber(ARGV[3])

-- Get current state
local data = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(data[1] or max_tokens)
local last_refill = tonumber(data[2] or current_time)

-- Calculate time elapsed since last refill
local elapsed = current_time - last_refill

-- Refill tokens based on elapsed time
if elapsed > 0 then
    local refill_rate = max_tokens / window_seconds
    local new_tokens = math.floor(elapsed * refill_rate)
    tokens = math.min(max_tokens, tokens + new_tokens)
    last_refill = current_time
end

-- Check if we have enough tokens
if tokens >= 1 then
    -- Consume one token
    tokens = tokens - 1
    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
    redis.call('EXPIRE', key, window_seconds)
    return 1
else
    return 0
end
```

### Rate Limiting Flow

```
Request → JWT Filter → Rate Limit Filter → Rate Limiter → Redis Lua Script → Decision
```

1. **JWT Filter**: Validates JWT token, extracts tenant ID
2. **Rate Limit Filter**: Constructs rate limit key (tenant:endpoint)
3. **Rate Limiter Factory**: Selects appropriate algorithm
4. **Algorithm**: Executes Redis Lua script
5. **Redis**: Atomic rate limit check and update
6. **Decision**: Allow or deny request based on result

## Authentication Architecture

### JWT Flow

```
1. Client requests token from Admin Service
2. Admin Service validates credentials
3. Admin Service generates JWT token
4. Client includes token in Authorization header
5. Gateway validates token using JwtUtil
6. Request proceeds to downstream service
```

### JWT Configuration

- **Algorithm**: HS256
- **Secret**: Configurable via application.yml
- **Expiration**: Configurable (default 24 hours)
- **Claims**: tenant ID, expiration, issued at

## Circuit Breaking Architecture

### Resilience4j Configuration

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

### Circuit Breaker States

1. **Closed**: Normal operation, requests pass through
2. **Open**: Circuit is tripped, requests fail fast
3. **Half-Open**: Testing if service has recovered

## Caching Architecture

### Redis Config Cache

The Admin Service uses Redis for caching configuration:

- **Tenant Cache**: Cache tenant data by ID
- **Rule Cache**: Cache rate limit rules by tenant ID
- **Cache Invalidation**: Automatic invalidation on updates/deletes

### Cache Strategy

- **Cache-First**: Check cache first, fallback to database
- **Write-Through**: Update cache on database updates
- **Time-to-Live**: Configurable TTL for cache entries

## Monitoring Architecture

### Metrics Collection

**Gateway Service Metrics**:
- HTTP request count and duration
- Rate limit decisions (allowed/denied)
- Circuit breaker state transitions
- Custom business metrics

**Admin Service Metrics**:
- API endpoint performance
- Database query performance
- Cache hit/miss ratios

### Prometheus Integration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

### Grafana Dashboard

Pre-configured dashboards for:
- Gateway performance
- Rate limiting statistics
- Circuit breaker status
- System resource usage

## Data Model

### Tenant Entity

```java
@Entity
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String apiKey;
    private String contactEmail;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### RateLimitRule Entity

```java
@Entity
public class RateLimitRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long tenantId;
    private String endpoint;
    private AlgorithmType algorithm;
    private int maxRequests;
    private int windowSeconds;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### RateLimitEvent Entity

```java
@Entity
public class RateLimitEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long tenantId;
    private String endpoint;
    private String apiKey;
    private boolean allowed;
    private String algorithm;
    private LocalDateTime timestamp;
}
```

## Database Schema

### Tables

1. **tenants**: Tenant configuration
2. **rate_limit_rules**: Rate limiting rules
3. **tier_defaults**: Default rate limits for tiers
4. **rate_limit_events**: Rate limit event log

### Flyway Migrations

All schema changes are managed through Flyway migrations in `sentinel-admin/src/main/resources/db/migration/`.

## Security Architecture

### Authentication

- **JWT-based authentication** for all API calls
- **API Key generation** per tenant
- **Token validation** in gateway filter
- **Secret rotation** support

### Authorization

- **Tenant-based isolation**: Each tenant has isolated rate limits
- **Endpoint-based rules**: Different limits per endpoint
- **Admin API protection**: Role-based access control

## Performance Considerations

### Rate Limiting Performance

- **Redis Lua scripts**: Atomic operations for high performance
- **Connection pooling**: Redis connection pool optimization
- **Batch operations**: Minimize Redis round trips
- **Memory usage**: Efficient data structures in Redis

### Circuit Breaking Performance

- **Fast failure**: Circuit breaker prevents cascading failures
- **Automatic recovery**: Automatic state transitions
- **Configurable thresholds**: Tunable for different services

### Caching Performance

- **Redis caching**: Reduces database load
- **Cache invalidation**: Automatic cache updates
- **TTL configuration**: Balance between freshness and performance

## Scalability

### Horizontal Scaling

- **Stateless gateway**: Multiple gateway instances
- **Redis clustering**: Distributed rate limiting state
- **Database scaling**: Connection pooling and optimization

### Vertical Scaling

- **CPU optimization**: Efficient Lua scripts
- **Memory optimization**: Efficient data structures
- **I/O optimization**: Connection pooling and batching

## Deployment Architecture

### Docker Compose Deployment

```yaml
services:
  sentinel-gateway:
    image: sentinel-gateway:latest
    ports:
      - "8080:8080"
    depends_on:
      - redis
      - admin-service
  
  sentinel-admin:
    image: sentinel-admin:latest
    ports:
      - "8081:8081"
    depends_on:
      - postgres
      - redis
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  postgres:
    image: postgres:15
    ports:
      - "5432:5432"
```

### Kubernetes Deployment

- **Stateless services**: Gateway and Admin services
- **Stateful services**: Redis and PostgreSQL with persistent volumes
- **Service discovery**: Kubernetes Service resources
- **Config management**: ConfigMaps and Secrets

## Error Handling

### Rate Limit Errors

- **429 Too Many Requests**: Rate limit exceeded
- **Headers**: Include rate limit information
- **Retry-After**: Suggested retry time

### Circuit Breaker Errors

- **503 Service Unavailable**: Circuit breaker open
- **Fallback**: Configurable fallback responses
- **Recovery**: Automatic recovery attempts

### System Errors

- **500 Internal Server Error**: Unexpected failures
- **Logging**: Comprehensive error logging
- **Monitoring**: Error rate tracking

## Testing Strategy

### Unit Tests

- **Entity tests**: JPA entity validation
- **Service tests**: Business logic validation
- **Repository tests**: Data access validation
- **Algorithm tests**: Rate limiting algorithm validation

### Integration Tests

- **Service integration**: End-to-end service tests
- **Database integration**: Database and JPA tests
- **Redis integration**: Redis and caching tests
- **Gateway integration**: Gateway routing tests

### Load Tests

- **Gatling scenarios**: Performance test scenarios
- **Rate limit tests**: Rate limiting under load
- **Circuit breaker tests**: Circuit breaker under failure
- **End-to-end tests**: Full system under load

## Future Enhancements

### Planned Features

- **Additional algorithms**: Leaky bucket, hierarchical token bucket
- **Advanced monitoring**: Distributed tracing with OpenTelemetry
- **API versioning**: Support for multiple API versions
- **Webhook notifications**: Rate limit alerts
- **Dynamic configuration**: Runtime algorithm changes
- **Multi-region deployment**: Geographic distribution

### Extensibility Points

- **Custom algorithms**: Implement RateLimiter interface
- **Custom filters**: Implement GatewayFilter interface
- **Custom metrics**: Add custom Prometheus metrics
- **Custom policies**: Implement custom rate limit policies