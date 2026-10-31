package com.sentinel.admin.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@EnabledIfSystemProperty(named = "docker.tests.enabled", matches = "true")
class FlywayMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sentinel")
            .withUsername("sentinel")
            .withPassword("sentinel123");

    @Test
    void testFlywayMigrations() throws Exception {
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        // Configure Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .load();
        
        // Run migrations
        var migrationsApplied = flyway.migrate();
        assertTrue(migrationsApplied.migrationsExecuted > 0, "At least one migration should be applied");

        // Verify schema was created
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            
            // Check tenants table exists
            ResultSet rs = stmt.executeQuery(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'tenants')");
            assertTrue(rs.next() && rs.getBoolean(1), "Tenants table should exist");

            // Check rate_limit_rules table exists
            rs = stmt.executeQuery(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'rate_limit_rules')");
            assertTrue(rs.next() && rs.getBoolean(1), "Rate limit rules table should exist");

            // Check tier_defaults table exists
            rs = stmt.executeQuery(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'tier_defaults')");
            assertTrue(rs.next() && rs.getBoolean(1), "Tier defaults table should exist");

            // Check rate_limit_events table exists
            rs = stmt.executeQuery(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'rate_limit_events')");
            assertTrue(rs.next() && rs.getBoolean(1), "Rate limit events table should exist");

            // Verify tier defaults were inserted
            rs = stmt.executeQuery("SELECT COUNT(*) FROM tier_defaults");
            assertTrue(rs.next() && rs.getInt(1) == 4, "4 tier defaults should be inserted");

            // Verify constraints exist
            rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.check_constraints WHERE constraint_name LIKE '%tier%'");
            assertTrue(rs.next() && rs.getInt(1) > 0, "Tier check constraints should exist");
        }
    }

    @Test
    void testMigrationValidation() throws Exception {
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        // Configure Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .load();
        
        flyway.migrate();

        // Validate migrations
        flyway.validate();
    }
}