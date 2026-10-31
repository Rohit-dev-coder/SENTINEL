# Starter demo

A tiny Spring Boot app that uses `sentinel-spring-boot-starter`. It shows the whole integration: one dependency, a
few lines of `application.yml`, and requests are rate limited per client.

It replaces the Redis limiters with an in-memory one (see `DemoApplication`), so you can run it without Redis. A real
application would delete that `@Bean` and set `spring.data.redis.*`.

```bash
# from the Sentinel repository root, install the library into your local Maven cache once
mvn install -DskipTests -pl sentinel-common,sentinel-limiter-core,sentinel-spring-boot-starter -am

cd examples/starter-demo
mvn package -DskipTests
java -jar target/starter-demo-1.0.0.jar
```

In another terminal (the default limit is 5 requests per minute, `/api/orders/**` allows 2):

```bash
for i in 1 2 3 4 5 6; do curl -s -o /dev/null -w "%{http_code} " -H "X-API-Key: alice" localhost:18090/api/hello; done
# 200 200 200 200 200 429
curl -i -H "X-API-Key: alice" localhost:18090/api/hello    # shows X-RateLimit-* and Retry-After
```
