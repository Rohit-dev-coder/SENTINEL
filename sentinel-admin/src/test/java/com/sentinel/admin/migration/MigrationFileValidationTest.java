package com.sentinel.admin.migration;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MigrationFileValidationTest {

    private static final String MIGRATION_PATH = "src/main/resources/db/migration";

    @Test
    void testMigrationFilesExist() throws IOException {
        Path migrationPath = Paths.get(MIGRATION_PATH);
        assertTrue(Files.exists(migrationPath), "Migration directory should exist");

        try (Stream<Path> paths = Files.walk(migrationPath)) {
            List<Path> migrationFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .toList();

            assertTrue(migrationFiles.size() >= 4, 
                    "At least 4 migration files should exist, found: " + migrationFiles.size());
        }
    }

    @Test
    void testMigrationFileNaming() throws IOException {
        Path migrationPath = Paths.get(MIGRATION_PATH);
        
        try (Stream<Path> paths = Files.walk(migrationPath)) {
            List<Path> migrationFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .toList();

            for (Path file : migrationFiles) {
                String fileName = file.getFileName().toString();
                assertTrue(fileName.matches("V\\d+__.*\\.sql"), 
                        "Migration file should follow naming pattern V<number>__<description>.sql: " + fileName);
            }
        }
    }

    @Test
    void testTenantsTableMigration() throws IOException {
        Path migrationPath = Paths.get(MIGRATION_PATH, "V1__Create_Tenants_Table.sql");
        assertTrue(Files.exists(migrationPath), "Tenants table migration should exist");

        List<String> lines = Files.readAllLines(migrationPath);
        String content = String.join("\n", lines);

        assertTrue(content.contains("CREATE TABLE tenants"), "Should create tenants table");
        assertTrue(content.contains("api_key"), "Should have api_key column");
        assertTrue(content.contains("tier"), "Should have tier column");
        assertTrue(content.contains("CHECK (tier IN"), "Should have tier check constraint");
        assertTrue(content.contains("idx_tenants_api_key"), "Should have api_key index");
    }

    @Test
    void testRateLimitRulesMigration() throws IOException {
        Path migrationPath = Paths.get(MIGRATION_PATH, "V2__Create_Rate_Limit_Rules_Table.sql");
        assertTrue(Files.exists(migrationPath), "Rate limit rules migration should exist");

        List<String> lines = Files.readAllLines(migrationPath);
        String content = String.join("\n", lines);

        assertTrue(content.contains("CREATE TABLE rate_limit_rules"), "Should create rate_limit_rules table");
        assertTrue(content.contains("tenant_id"), "Should have tenant_id column");
        assertTrue(content.contains("endpoint_pattern"), "Should have endpoint_pattern column");
        assertTrue(content.contains("algorithm"), "Should have algorithm column");
        assertTrue(content.contains("max_requests"), "Should have max_requests column");
        assertTrue(content.contains("CHECK (algorithm IN"), "Should have algorithm check constraint");
        assertTrue(content.contains("REFERENCES"), "Should have foreign key constraint");
    }

    @Test
    void testTierDefaultsMigration() throws IOException {
        Path migrationPath = Paths.get(MIGRATION_PATH, "V3__Create_Tier_Defaults_Table.sql");
        assertTrue(Files.exists(migrationPath), "Tier defaults migration should exist");

        List<String> lines = Files.readAllLines(migrationPath);
        String content = String.join("\n", lines);

        assertTrue(content.contains("CREATE TABLE tier_defaults"), "Should create tier_defaults table");
        assertTrue(content.contains("INSERT INTO tier_defaults"), "Should insert default tier data");
        assertTrue(content.contains("FREE"), "Should include FREE tier");
        assertTrue(content.contains("BASIC"), "Should include BASIC tier");
        assertTrue(content.contains("PREMIUM"), "Should include PREMIUM tier");
        assertTrue(content.contains("ENTERPRISE"), "Should include ENTERPRISE tier");
    }

    @Test
    void testRateLimitEventsMigration() throws IOException {
        Path migrationPath = Paths.get(MIGRATION_PATH, "V4__Create_Rate_Limit_Events_Table.sql");
        assertTrue(Files.exists(migrationPath), "Rate limit events migration should exist");

        List<String> lines = Files.readAllLines(migrationPath);
        String content = String.join("\n", lines);

        assertTrue(content.contains("CREATE TABLE rate_limit_events"), "Should create rate_limit_events table");
        assertTrue(content.contains("tenant_id"), "Should have tenant_id column");
        assertTrue(content.contains("allowed"), "Should have allowed column");
        assertTrue(content.contains("created_at"), "Should have created_at column");
        assertTrue(content.contains("idx_events_tenant"), "Should have tenant index");
        assertTrue(content.contains("idx_events_time"), "Should have time index");
    }

    @Test
    void testMigrationOrdering() throws IOException {
        Path migrationPath = Paths.get(MIGRATION_PATH);
        
        try (Stream<Path> paths = Files.walk(migrationPath)) {
            List<Path> migrationFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .sorted()
                    .toList();

            // Verify migration sequence
            assertTrue(migrationFiles.get(0).getFileName().toString().startsWith("V1__"), 
                    "First migration should be V1");
            assertTrue(migrationFiles.get(1).getFileName().toString().startsWith("V2__"), 
                    "Second migration should be V2");
            assertTrue(migrationFiles.get(2).getFileName().toString().startsWith("V3__"), 
                    "Third migration should be V3");
            assertTrue(migrationFiles.get(3).getFileName().toString().startsWith("V4__"), 
                    "Fourth migration should be V4");
        }
    }
}