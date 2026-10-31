package com.sentinel.downstream.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SampleController {

    @GetMapping("/users")
    public Map<String, Object> getUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "sample-service-1");
        response.put("endpoint", "/api/v1/users");
        response.put("message", "Users endpoint from Sample Service 1");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/products")
    public Map<String, Object> getProducts() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "sample-service-1");
        response.put("endpoint", "/api/v1/products");
        response.put("message", "Products endpoint from Sample Service 1");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/orders")
    public Map<String, Object> getOrders() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "sample-service-1");
        response.put("endpoint", "/api/v1/orders");
        response.put("message", "Orders endpoint from Sample Service 1");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "sample-service-1");
        return response;
    }
}