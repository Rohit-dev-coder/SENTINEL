# Sentinel Spring Boot Starter

Rate limiting for Spring MVC applications. Add the dependency, set a few properties, and every request is limited per
client and per endpoint. Counters live in Redis, so the limit is shared across every instance of your service.

**Contents:** [Requirements](#requirements) · [Add the dependency](#1-add-the-dependency) · [Configure Redis](#2-point-spring-at-redis) · [Try it](#3-start-the-app-and-check-it-works) · [Configure limits](#configure-limits) · [All properties](#all-properties) · [Change settings per environment](#changing-settings-in-different-ways) · [What clients see](#what-clients-see) · [Extend or replace parts](#extending-and-replacing-parts) · [Troubleshooting](#troubleshooting)

## Requirements

| You need | Notes |
|---|---|
| Java 17+ | |
| Spring Boot 3.x | Built against 3.2 |
| A servlet web app (Spring MVC) | Reactive (WebFlux) apps are not covered; use the Sentinel gateway for those |
| A Redis server | The starter brings the Redis client; you supply the server |

## 1. Add the dependency

**Maven**

```xml
<dependency>
    <groupId>com.sentinel</groupId>
    <artifactId>sentinel-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle (Groovy)**

```groovy
implementation 'com.sentinel:sentinel-spring-boot-starter:1.0.0'
```

**Gradle (Kotlin)**

```kotlin
implementation("com.sentinel:sentinel-spring-boot-starter:1.0.0")
```

You also need Spring MVC, which most web apps already have (`spring-boot-starter-web`). Nothing else to add: the
auto-configuration registers itself.

> **Before the library is on Maven Central** you can install it into your own machine's Maven cache and use it from
> there. From the Sentinel repository:
>
> ```bash
> mvn install -DskipTests -pl sentinel-common,sentinel-limiter-core,sentinel-spring-boot-starter -am
> ```
>
> Then add the dependency above to your project as usual (Gradle users also add `mavenLocal()` to `repositories`).

## 2. Point Spring at Redis

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # password: ${REDIS_PASSWORD}
      # ssl:
      #   enabled: true
      # database: 3        # see "Sharing a Redis server" below
```

These are the normal Spring Boot Redis settings; the starter uses whatever Redis connection your app already has.

## 3. Start the app and check it works

With no other settings, every client gets **100 requests per minute** (token bucket). Clients are told apart by the
`X-API-Key` request header, or by IP address when the header is missing.

To see a limit trip quickly, add this and restart:

```yaml
sentinel:
  default-limit:
    max-requests: 3
    window-seconds: 60
```

```bash
for i in 1 2 3 4 5; do curl -s -o /dev/null -w "%{http_code}\n" -H "X-API-Key: demo" localhost:8080/your/endpoint; done
```

Expected: three normal answers, then `429`. Look at the headers with `curl -i`: `X-RateLimit-Remaining` counts down and
`Retry-After` appears on the `429`.

## Configure limits

There are two ways to decide the limits. Use one.

### Standalone mode: limits in your own configuration (default)

```yaml
sentinel:
  default-limit:                    # used when no rule matches
    algorithm: TOKEN_BUCKET         # TOKEN_BUCKET | FIXED_WINDOW_COUNTER | SLIDING_WINDOW_LOG | SLIDING_WINDOW_COUNTER
    max-requests: 100
    window-seconds: 60
  rules:                            # the first matching rule wins, so put specific rules first
    - path-pattern: /api/orders/**
      algorithm: FIXED_WINDOW_COUNTER
      max-requests: 5
      window-seconds: 30
    - path-pattern: /api/reports/*
      max-requests: 20
      window-seconds: 60
```

Path patterns are Ant-style: `*` matches one path segment, `**` matches any depth, and the pattern must start with `/`.
Each client has its own counter **per method and path**, for example `GET /api/orders/1` and `GET /api/orders/2` are separate.

### Connected mode: limits managed in a Sentinel admin service

Set the admin service address and the starter fetches each tenant's tier and rules from it, so you change limits on the
Sentinel dashboard without redeploying your app.

```yaml
sentinel:
  admin:
    url: https://sentinel-admin.example.com
    api-key: ${SENTINEL_ADMIN_KEY}
    cache-ttl: 30s      # how long a tenant's settings are remembered
    timeout: 2s         # connect/read timeout for the admin service
```

In connected mode:
- the `X-API-Key` header must carry the **tenant API key** created in the Sentinel dashboard;
- unknown or deactivated tenants get `403`, and a missing key gets `401`;
- `sentinel.default-limit` and `sentinel.rules` are ignored (the tenant's tier and rules decide);
- if the admin service is unreachable, the last known settings are used, or FREE-tier limits if nothing is cached yet.

## All properties

| Property | Default | Meaning |
|---|---|---|
| `sentinel.enabled` | `true` | Master switch for the whole starter |
| `sentinel.client-id-header` | `X-API-Key` | Request header that identifies the caller (the tenant API key in connected mode) |
| `sentinel.identity-fallback` | `CLIENT_IP` | When the header is missing: `CLIENT_IP` or `REJECT` (answers 401). Standalone mode only |
| `sentinel.fail-open` | `true` | If Redis fails: `true` lets the request through and logs an error, `false` answers 503 |
| `sentinel.filter-order` | `HIGHEST_PRECEDENCE + 10` | Servlet filter order; lower runs earlier |
| `sentinel.exclude-paths` | `/actuator/**`, `/error` | Ant paths that are never limited. Setting this **replaces** the list, so repeat the defaults you still want |
| `sentinel.default-limit.algorithm` | `TOKEN_BUCKET` | Algorithm when no rule matches |
| `sentinel.default-limit.max-requests` | `100` | Requests allowed per window (at least 1) |
| `sentinel.default-limit.window-seconds` | `60` | Window length in seconds (at least 1) |
| `sentinel.rules[n].path-pattern` | none | Required; Ant pattern starting with `/` |
| `sentinel.rules[n].algorithm` | `TOKEN_BUCKET` | Algorithm for that pattern |
| `sentinel.rules[n].max-requests` | `100` | Requests per window for that pattern |
| `sentinel.rules[n].window-seconds` | `60` | Window length for that pattern |
| `sentinel.admin.url` | none | Setting this turns on connected mode |
| `sentinel.admin.api-key` | none | Sent to the admin service as `X-Admin-Key` |
| `sentinel.admin.cache-ttl` | `30s` | How long a tenant's settings are cached |
| `sentinel.admin.timeout` | `2s` | Connect and read timeout for the admin service |

Invalid values (for example `max-requests: 0`, or a rule pattern without a leading `/`) stop the application at startup
with a clear message instead of failing later.

### The four algorithms

| Algorithm | Behaviour | Good for | Watch out for |
|---|---|---|---|
| `TOKEN_BUCKET` | Allows a short burst, then refills at a steady rate | Most APIs | |
| `FIXED_WINDOW_COUNTER` | Counts requests per calendar window | Cheapest, simplest | A client can send up to double the limit across a window boundary |
| `SLIDING_WINDOW_LOG` | Remembers every request time in the last window | Exact limits | Memory grows with traffic |
| `SLIDING_WINDOW_COUNTER` | Counts per 10-second slice and adds them up | Near-exact limits, small memory | Precision is about 10 seconds |

## Changing settings in different ways

All `sentinel.*` settings follow normal Spring Boot rules, so you can set them anywhere Spring reads configuration.

**`application.properties`**

```properties
sentinel.default-limit.max-requests=50
sentinel.rules[0].path-pattern=/api/orders/**
sentinel.rules[0].max-requests=5
```

**Environment variables** (handy in containers; `-` becomes `_`)

```bash
SENTINEL_ENABLED=true
SENTINEL_DEFAULT_LIMIT_MAX_REQUESTS=50
SENTINEL_ADMIN_URL=https://sentinel-admin.example.com
SENTINEL_ADMIN_API_KEY=...
```

Use YAML or properties files for the `rules` list; it is awkward to express as environment variables.

**Command line:** `java -jar app.jar --sentinel.default-limit.max-requests=50`

**Per environment:** put overrides in `application-dev.yml`, `application-prod.yml` and so on. A common choice is turning it
off locally:

```yaml
# application-dev.yml
sentinel:
  enabled: false
```

**IDE auto-completion:** the jar ships Spring configuration metadata, so IntelliJ and VS Code suggest every
`sentinel.*` key and show its description and default.

## What clients see

| Situation | Response |
|---|---|
| Allowed | Your normal response, plus `X-RateLimit-Limit`, `X-RateLimit-Remaining` and `X-RateLimit-Reset` (epoch seconds) |
| Limit reached | `429`, with `Retry-After` (seconds) and body `{"error":"rate_limit_exceeded"}` |
| No client id and `identity-fallback: REJECT` | `401`, body `{"error":"missing_client_id"}` |
| Connected mode: unknown or inactive tenant | `403`, body `{"error":"unknown_or_inactive_tenant"}` |
| Redis down | Request allowed and an error logged (`fail-open: true`); `503` if `fail-open: false` |

Preflight (`OPTIONS`) requests are never limited.

## Extending and replacing parts

Everything the starter creates backs off if you define your own bean of the same type, so you can replace any piece
by declaring a `@Bean` in your application.

| To change | Declare a bean of type | Notes |
|---|---|---|
| How a limit is stored or counted (for example an in-memory limiter for tests) | `com.sentinel.limiter.RateLimiterFactory` | Its constructor takes one `RateLimiter` per algorithm. `RateLimiter` is a one-method interface: `allowRequest(key, maxRequests, windowSeconds)` returning a `RateLimitResult` |
| How the limit for a path is picked | `com.sentinel.starter.LimitResolver` | |
| The whole filter | `com.sentinel.starter.SentinelRateLimitFilter` | |

Example: use an in-memory limiter in tests, without Redis.

```java
@Bean
RateLimiterFactory rateLimiterFactory() {
    RateLimiter inMemory = (key, max, windowSeconds) -> /* your logic */ new RateLimitResult(true, max, 0L);
    return new RateLimiterFactory(inMemory, inMemory, inMemory, inMemory);
}
```

What cannot be changed with configuration alone today:
- **Which request header or value identifies the client.** You can change the header name (`sentinel.client-id-header`),
  but not read the identity from somewhere else, such as a JWT claim or the logged-in user. That would need a custom filter.
- **Adding a fifth algorithm.** The four algorithms are fixed in `AlgorithmType`. You can replace the implementation of
  any of the four through `RateLimiterFactory`.
- **Servlet only.** Reactive applications are not supported.

## Sharing a Redis server

Counter keys look like `<algorithm>:<client>:<METHOD>:<path>`. If two different applications use the **same Redis
database** and the same client ids and paths, they will share counters. Give each application its own Redis database
(`spring.data.redis.database`) or its own Redis server.

## Behind a proxy or load balancer

IP-based identification uses the connection's remote address, which behind a proxy is the proxy itself. Enable Spring's
forwarded-header handling so the real client address is used:

```yaml
server:
  forward-headers-strategy: framework
```

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Nothing is limited | `sentinel.enabled=false`; the app is not a servlet web app; or the path matches `sentinel.exclude-paths` |
| Everything is limited together | No `X-API-Key` header and all clients share one proxy IP; set the header or fix forwarded headers |
| Log says "Rate limiter unavailable, allowing request" | Cannot reach Redis. Check `spring.data.redis.*`. Requests pass because `fail-open` is `true` |
| Application fails at startup with "sentinel...must be at least 1" | A limit value is 0 or negative |
| Connected mode returns `403` for a valid customer | The tenant is deactivated in the dashboard, or the key is wrong |
| Connected mode returns `401` | The request has no client id header |
| Different apps interfere with each other | They share a Redis database; see "Sharing a Redis server" |
| Limits did not change after editing them in the dashboard | Connected mode caches a tenant for `sentinel.admin.cache-ttl` (30 s by default) |

## Under the hood

The starter contains one servlet filter, `SentinelRateLimitFilter`, registered automatically through Spring Boot
auto-configuration. For each request it works out the client id and the limit, asks the Redis-backed limiter
(`sentinel-limiter-core`, atomic Lua scripts) whether the request may pass, sets the `X-RateLimit-*` headers, and either
continues the request or answers `429`.
