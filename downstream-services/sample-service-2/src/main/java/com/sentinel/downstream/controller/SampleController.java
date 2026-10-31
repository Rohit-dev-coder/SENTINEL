package com.sentinel.downstream.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SampleController {

    @GetMapping("/analytics")
    public Map<String, Object> getAnalytics() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "sample-service-2");
        response.put("endpoint", "/api/v1/analytics");
        response.put("message", "Analytics endpoint from Sample Service 2");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/reports")
    public Map<String, Object> getReports() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "sample-service-2");
        response.put("endpoint", "/api/v1/reports");
        response.put("message", "Reports endpoint from Sample Service 2");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "sample-service-2");
        response.put("endpoint", "/api/v1/dashboard");
        response.put("message", "Dashboard endpoint from Sample Service 2");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "sample-service-2");
        return response;
    }
}