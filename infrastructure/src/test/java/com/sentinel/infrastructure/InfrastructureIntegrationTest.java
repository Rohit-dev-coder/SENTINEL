package com.sentinel.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@EnabledIfSystemProperty(named = "docker.tests.enabled", matches = "true")
class InfrastructureIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sentinel")
            .withUsername("sentinel")
            .withPassword("sentinel123");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Test
    void testPostgreSQLConnection() throws Exception {
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            
            // Test basic query
            ResultSet rs = stmt.executeQuery("SELECT 1");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
            
            // Test database exists
            rs = stmt.executeQuery("SELECT current_database()");
            assertTrue(rs.next());
            assertEquals("sentinel", rs.getString(1));
        }
    }

    @Test
    void testRedisConnection() {
        String redisHost = redis.getHost();
        Integer redisPort = redis.getMappedPort(6379);

        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            // Test basic operations
            jedis.set("test_key", "test_value");
            assertEquals("test_value", jedis.get("test_key"));
            
            // Test increment
            jedis.del("counter");
            assertEquals(1L, jedis.incr("counter"));
            assertEquals(2L, jedis.incr("counter"));
            
            // Test expiration
            jedis.setex("temp_key", 1, "temp_value");
            assertEquals("temp_value", jedis.get("temp_key"));
        }
    }

    @Test
    void testInfrastructureHealth() {
        // Verify both containers are healthy
        assertTrue(postgres.isRunning(), "PostgreSQL should be running");
        assertTrue(redis.isRunning(), "Redis should be running");
        
        // Verify ports are accessible
        assertNotNull(postgres.getMappedPort(5432), "PostgreSQL port should be mapped");
        assertNotNull(redis.getMappedPort(6379), "Redis port should be mapped");
    }
}