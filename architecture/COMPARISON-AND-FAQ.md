# SENTINEL: comparison with similar tools, and common questions

Read the [KT guide](KT-guide.md) first for how the system works. This page answers the questions people ask next.

---

## 1. How is SENTINEL different from Spring Cloud Gateway?

SENTINEL is **not an alternative** to Spring Cloud Gateway. It is **built on it**. Spring Cloud Gateway does the routing and filtering. SENTINEL adds filters, services and a dashboard on top.

| | Spring Cloud Gateway alone | SENTINEL |
|---|---|---|
| **Routing and filters** | Yes, this is its job | Uses it unchanged |
| **Rate limiting** | One built-in `RequestRateLimiter`: a Redis token bucket with a fixed rate and burst set per route in configuration | Four algorithms (token bucket, fixed window, sliding window log, sliding window counter) as custom Lua scripts |
| **Who gets which limit** | You write a key resolver (for example by IP address or user) | Limits per tenant, set by tier, plus per-endpoint rules |
| **Changing a limit** | Edit configuration, then redeploy or refresh | Edit on the dashboard; the gateway picks it up within 30 seconds |
| **Customers and API keys** | Not included | Tenant records, API keys, and `POST /auth/token` to exchange a key for a JWT |
| **Admin tooling** | None | An admin REST API (PostgreSQL) and a web dashboard |
| **Resilience, tracing, metrics** | Provides the building blocks | Circuit breaker and retry wired into each route, request IDs, Prometheus on a separate port |

**In one sentence:** Spring Cloud Gateway is the engine; SENTINEL is a multi-tenant product built around that engine, for a team that runs an API platform with different customers on different plans.

### When to choose which

| Situation | Choice |
|---|---|
| "100 requests per minute per user" on a few routes | Plain Spring Cloud Gateway with its built-in limiter is simpler |
| Different limits per customer plan, changed without redeploying | SENTINEL |
| Customers need API keys and tokens, and operators need a console | SENTINEL |
| Route management from a UI, developer portal, billing, analytics | A full API management product (for example Kong or Apigee); SENTINEL does not cover these |

### What SENTINEL does not add

- **Routes are still fixed.** They live in `sentinel-gateway/src/main/resources/application.yml` and cannot be managed from the dashboard.
- **It is not proven in production.** See the status notes in the [KT guide](KT-guide.md#10-honest-status-what-is-not-finished).
- **You could build it yourself.** Everything above is possible with plain Spring Cloud Gateway, a custom rate limiter and a small admin app. SENTINEL is that work already done and joined together.

---

## 2. How is SENTINEL different from Spring AI?

They solve unrelated problems.

| | Spring AI | SENTINEL |
|---|---|---|
| **Purpose** | Helps you build applications that call language models: chat, embeddings, vector search, tool calling | Sits in front of any API and decides who may call it and how often |
| **Where it runs** | Inside your application | As a separate gateway in front of your applications |

They can work together: SENTINEL could sit in front of an LLM-backed API and throttle each customer. Note that SENTINEL counts **requests**, not **tokens**, so it cannot cap token usage today.

---

## 3. How does a customer use SENTINEL?

"Customer" can mean two different things.

### A. A company whose apps call your APIs (an API consumer)

They need no SENTINEL software.

1. You create their tenant on the dashboard and send them the API key.
2. They call `POST /auth/token` with the header `X-Tenant-Api-Key: <key>` and receive a JWT valid for 24 hours.
3. They call your API through the gateway with `Authorization: Bearer <jwt>`.
4. When they receive `429`, they wait and retry. The `X-RateLimit-Limit`, `X-RateLimit-Remaining` and `X-RateLimit-Reset` headers tell them where they stand.

Known rough edges to fix before sharing with outside developers:

- There is no `Retry-After` header.
- `X-RateLimit-Reset` is in **seconds** for some algorithms and **milliseconds** for the sliding window counter.

### B. A company that wants to run SENTINEL for its own APIs

Today they would clone the repository and run `docker-compose up` (see Part 7 of the KT guide). Limits to know:

- Gateway routes are fixed in `application.yml` and point only at the two sample services. A real deployment must edit that file and rebuild.
- No Docker images are published yet.

---

## 4. Can SENTINEL be added to an application as a dependency?

Yes. The `sentinel-spring-boot-starter` library does this for Spring MVC applications: add the dependency, point
Spring at Redis, and requests are rate limited. Limits can come from the application's own properties, or be managed
centrally by a SENTINEL admin service. Setup and all properties: [`sentinel-spring-boot-starter/README.md`](../sentinel-spring-boot-starter/README.md).

```xml
<dependency>
  <groupId>com.sentinel</groupId>
  <artifactId>sentinel-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

The gateway and admin applications are **not** libraries: they are Spring Boot executable JARs, meant to be run, and are
shipped as Docker images. The library modules are `sentinel-common`, `sentinel-limiter-core` and the starter.

The POMs are prepared for Maven Central, but nothing has been published. Steps and open decisions (license, namespace,
version, employer approval): [`PUBLISHING.md`](PUBLISHING.md).
