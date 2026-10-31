package com.sentinel.downstream;

import com.sentinel.downstream.controller.SampleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SampleService1Test {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("sample-service-1"))
                .andExpect(jsonPath("$.endpoint").value("/api/v1/users"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testProductsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("sample-service-1"))
                .andExpect(jsonPath("$.endpoint").value("/api/v1/products"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testOrdersEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("sample-service-1"))
                .andExpect(jsonPath("$.endpoint").value("/api/v1/orders"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("sample-service-1"));
    }
}