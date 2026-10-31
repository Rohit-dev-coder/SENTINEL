package com.sentinel.infrastructure;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CIConfigurationTest {

    private static final String PROJECT_ROOT = System.getProperty("user.dir").replace("infrastructure", "");

    @Test
    void testCIWorkflowFileExists() throws IOException {
        Path ciWorkflowPath = Paths.get(PROJECT_ROOT, ".github/workflows/ci.yml");
        assertTrue(Files.exists(ciWorkflowPath), "CI workflow file should exist");
    }

    @Test
    void testCIWorkflowContainsRequiredJobs() throws IOException {
        Path ciWorkflowPath = Paths.get(PROJECT_ROOT, ".github/workflows/ci.yml");
        List<String> lines = Files.readAllLines(ciWorkflowPath);
        String content = String.join("\n", lines);

        assertTrue(content.contains("build-and-test"), "Should have build-and-test job");
        assertTrue(content.contains("docker-build"), "Should have docker-build job");
        assertTrue(content.contains("infrastructure-test"), "Should have infrastructure-test job");
        assertTrue(content.contains("security-scan"), "Should have security-scan job");
        assertTrue(content.contains("code-quality"), "Should have code-quality job");
    }

    @Test
    void testCIWorkflowUsesJava17() throws IOException {
        Path ciWorkflowPath = Paths.get(PROJECT_ROOT, ".github/workflows/ci.yml");
        List<String> lines = Files.readAllLines(ciWorkflowPath);
        String content = String.join("\n", lines);

        assertTrue(content.contains("java-version: '17'"), "Should use Java 17");
    }

    @Test
    void testCIWorkflowRunsMavenTests() throws IOException {
        Path ciWorkflowPath = Paths.get(PROJECT_ROOT, ".github/workflows/ci.yml");
        List<String> lines = Files.readAllLines(ciWorkflowPath);
        String content = String.join("\n", lines);

        assertTrue(content.contains("mvn test"), "Should run Maven tests");
        assertTrue(content.contains("maven"), "Should use Maven");
    }

    @Test
    void testCIWorkflowBuildsDockerImages() throws IOException {
        Path ciWorkflowPath = Paths.get(PROJECT_ROOT, ".github/workflows/ci.yml");
        List<String> lines = Files.readAllLines(ciWorkflowPath);
        String content = String.join("\n", lines);

        assertTrue(content.contains("docker build"), "Should build Docker images");
        assertTrue(content.contains("sentinel-gateway"), "Should build gateway image");
        assertTrue(content.contains("sentinel-admin"), "Should build admin image");
    }

    @Test
    void testMavenHasTestPlugins() throws IOException {
        Path pomPath = Paths.get(PROJECT_ROOT, "pom.xml");
        List<String> lines = Files.readAllLines(pomPath);
        String content = String.join("\n", lines);

        assertTrue(content.contains("maven-surefire-plugin"), "Should have surefire plugin");
        assertTrue(content.contains("jacoco-maven-plugin"), "Should have jacoco plugin");
    }

    @Test
    void testValidationScriptExists() throws IOException {
        Path scriptPath = Paths.get(PROJECT_ROOT, "scripts/validate-ci-config.sh");
        assertTrue(Files.exists(scriptPath), "CI validation script should exist");
    }

    @Test
    void testValidationScriptIsExecutable() throws IOException {
        Path scriptPath = Paths.get(PROJECT_ROOT, "scripts/validate-ci-config.sh");
        assertTrue(Files.isExecutable(scriptPath), "CI validation script should be executable");
    }
}