package com.sentinel.infrastructure;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DockerComposeConfigurationTest {

    private static final String PROJECT_ROOT = System.getProperty("user.dir").replace("infrastructure", "");

    @Test
    void testDockerComposeFileExists() throws IOException {
        Path dockerComposePath = Paths.get(PROJECT_ROOT, "docker-compose.yml");
        assertTrue(Files.exists(dockerComposePath), "docker-compose.yml should exist at: " + dockerComposePath);
    }

    @Test
    void testDockerComposeContainsRequiredServices() throws IOException {
        Path dockerComposePath = Paths.get(PROJECT_ROOT, "docker-compose.yml");
        List<String> lines = Files.readAllLines(dockerComposePath);
        String content = String.join("\n", lines);

        // Verify required services are defined
        assertTrue(content.contains("redis:"), "Redis service should be defined");
        assertTrue(content.contains("postgresql:"), "PostgreSQL service should be defined");
        assertTrue(content.contains("sentinel-gateway:"), "Gateway service should be defined");
        assertTrue(content.contains("sentinel-admin:"), "Admin service should be defined");
        assertTrue(content.contains("prometheus:"), "Prometheus service should be defined");
        assertTrue(content.contains("grafana:"), "Grafana service should be defined");
    }

    @Test
    void testDockerComposeContainsHealthChecks() throws IOException {
        Path dockerComposePath = Paths.get(PROJECT_ROOT, "docker-compose.yml");
        List<String> lines = Files.readAllLines(dockerComposePath);
        String content = String.join("\n", lines);

        // Verify health checks are defined for critical services
        assertTrue(content.contains("healthcheck:"), "Health checks should be defined");
        assertTrue(content.contains("redis-cli"), "Redis health check should use redis-cli");
        assertTrue(content.contains("pg_isready"), "PostgreSQL health check should use pg_isready");
    }

    @Test
    void testPrometheusConfigExists() throws IOException {
        Path prometheusPath = Paths.get(PROJECT_ROOT, "infrastructure/monitoring/prometheus.yml");
        assertTrue(Files.exists(prometheusPath), "Prometheus config should exist at: " + prometheusPath);
    }

    @Test
    void testPrometheusConfigContainsRequiredJobs() throws IOException {
        Path prometheusPath = Paths.get(PROJECT_ROOT, "infrastructure/monitoring/prometheus.yml");
        List<String> lines = Files.readAllLines(prometheusPath);
        String content = String.join("\n", lines);

        // Verify required scrape jobs are defined
        assertTrue(content.contains("sentinel-gateway"), "Gateway scrape job should be defined");
        assertTrue(content.contains("sentinel-admin"), "Admin scrape job should be defined");


    }

    @Test
    void testDockerfilesExist() throws IOException {
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-gateway/Dockerfile")), "Gateway Dockerfile should exist");
        assertTrue(Files.exists(Paths.get(PROJECT_ROOT, "sentinel-admin/Dockerfile")), "Admin Dockerfile should exist");
    }

    @Test
    void testDockerfilesContainRequiredInstructions() throws IOException {
        // Check Gateway Dockerfile
        Path gatewayDockerfile = Paths.get(PROJECT_ROOT, "sentinel-gateway/Dockerfile");
        List<String> gatewayLines = Files.readAllLines(gatewayDockerfile);
        String gatewayContent = String.join("\n", gatewayLines);
        
        assertTrue(gatewayContent.contains("FROM"), "Dockerfile should have FROM instruction");
        assertTrue(gatewayContent.contains("COPY"), "Dockerfile should have COPY instruction");
        assertTrue(gatewayContent.contains("EXPOSE 8080"), "Gateway should expose port 8080");

        // Check Admin Dockerfile
        Path adminDockerfile = Paths.get(PROJECT_ROOT, "sentinel-admin/Dockerfile");
        List<String> adminLines = Files.readAllLines(adminDockerfile);
        String adminContent = String.join("\n", adminLines);
        
        assertTrue(adminContent.contains("FROM"), "Dockerfile should have FROM instruction");
        assertTrue(adminContent.contains("COPY"), "Dockerfile should have COPY instruction");
        assertTrue(adminContent.contains("EXPOSE 8081"), "Admin should expose port 8081");
    }
}