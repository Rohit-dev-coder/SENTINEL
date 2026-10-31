package com.sentinel.infrastructure;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProjectStructureTest {

    private static final String PROJECT_ROOT = System.getProperty("user.dir").replace("infrastructure", "");

    @Test
    void testProjectStructure() throws IOException {
        // Verify main project structure
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "pom.xml")), "Root pom.xml should exist");
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "docker-compose.yml")), "docker-compose.yml should exist");
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, ".gitignore")), ".gitignore should exist");
        
        // Verify module structure
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-common")), "Common module should exist");
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-gateway")), "Gateway module should exist");
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-admin")), "Admin module should exist");
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "infrastructure")), "Infrastructure module should exist");
    }

    @Test
    void testModuleStructure() throws IOException {
        // Verify each module has proper structure
        String[] modules = {"sentinel-common", "sentinel-gateway", "sentinel-admin"};
        
        for (String module : modules) {
            Path modulePath = Paths.get(PROJECT_ROOT, module);
            assertTrue(Files.exists(modulePath), module + " module should exist");
            assertTrue(Files.exists(Paths.get(modulePath.toString(), "pom.xml")), module + " pom.xml should exist");
            assertTrue(Files.exists(Paths.get(modulePath.toString(), "src/main/java")), module + " main source should exist");
            assertTrue(Files.exists(Paths.get(modulePath.toString(), "src/test/java")), module + " test source should exist");
        }
    }

    @Test
    void testCommonModuleStructure() throws IOException {
        Path commonPath = Paths.get(PROJECT_ROOT, "sentinel-common/src/main/java/com/sentinel/common");
        assertTrue(Files.exists(commonPath), "Common module package structure should exist");
        
        // Verify common module has expected packages
        assertTrue(Files.exists(Paths.get(commonPath.toString(), "enums")), "Enums package should exist");
        assertTrue(Files.exists(Paths.get(commonPath.toString(), "exception")), "Exception package should exist");
    }

    @Test
    void testAdminModuleStructure() throws IOException {
        Path adminPath = Paths.get(PROJECT_ROOT, "sentinel-admin/src/main/java/com/sentinel/admin");
        assertTrue(Files.exists(adminPath), "Admin module package structure should exist");
        
        // Verify admin module has expected directories
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-admin/src/main/resources/db/migration")), 
                "Migration directory should exist");
    }

    @Test
    void testInfrastructureModuleStructure() throws IOException {
        Path infraPath = Paths.get(PROJECT_ROOT, "infrastructure/src/test/java/com/sentinel/infrastructure");
        assertTrue(Files.exists(infraPath), "Infrastructure test package should exist");
    }

    @Test
    void testConfigurationFiles() throws IOException {
        // Verify configuration files exist
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "infrastructure/monitoring/prometheus.yml")), 
                "Prometheus config should exist");
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-gateway/Dockerfile")), 
                "Gateway Dockerfile should exist");
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-admin/Dockerfile")), 
                "Admin Dockerfile should exist");
    }

    @Test
    void testSourceFilesExist() throws IOException {
        // Verify source files exist in common module
        Path commonPath = Paths.get(PROJECT_ROOT, "sentinel-common/src/main/java/com/sentinel/common");
        assertTrue(Files.exists(Paths.get(commonPath.toString(), "enums/AlgorithmType.java")), 
                "AlgorithmType should exist");
        assertTrue(Files.exists(Paths.get(commonPath.toString(), "enums/Tier.java")), 
                "Tier should exist");
        assertTrue(Files.exists(Paths.get(commonPath.toString(), "exception/RateLimitExceededException.java")), 
                "RateLimitExceededException should exist");
        assertTrue(Files.exists(Paths.get(commonPath.toString(), "exception/UnauthorizedException.java")), 
                "UnauthorizedException should exist");
    }

    @Test
    void testTestFilesExist() throws IOException {
        // Verify test files exist
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-common/src/test/java/com/sentinel/common/enums/AlgorithmTypeTest.java")), 
                "AlgorithmTypeTest should exist");
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-common/src/test/java/com/sentinel/common/enums/TierTest.java")), 
                "TierTest should exist");
    }

    @Test
    void testMigrationFilesExist() throws IOException {
        Path migrationPath = Paths.get(PROJECT_ROOT, "sentinel-admin/src/main/resources/db/migration");
        
        try (Stream<Path> paths = Files.walk(migrationPath)) {
            long migrationCount = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .count();
            
            assertTrue(migrationCount >= 4, "At least 4 migration files should exist");
        }
    }

    @Test
    void testGitIgnoreFile() throws IOException {
        Path gitignorePath = Paths.get(PROJECT_ROOT, ".gitignore");
        assertTrue(Files.exists(gitignorePath), ".gitignore should exist");
        
        List<String> lines = Files.readAllLines(gitignorePath);
        String content = String.join("\n", lines);
        
        assertTrue(content.contains("target/"), "Should ignore target directory");
        assertTrue(content.contains(".idea/"), "Should ignore IDE files");
        assertTrue(content.contains("*.log"), "Should ignore log files");
    }
}