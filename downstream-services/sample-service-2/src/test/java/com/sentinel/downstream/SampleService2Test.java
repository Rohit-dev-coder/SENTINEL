package com.sentinel.downstream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SampleService2Test {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAnalyticsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("sample-service-2"))
                .andExpect(jsonPath("$.endpoint").value("/api/v1/analytics"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testReportsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("sample-service-2"))
                .andExpect(jsonPath("$.endpoint").value("/api/v1/reports"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testDashboardEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("sample-service-2"))
                .andExpect(jsonPath("$.endpoint").value("/api/v1/dashboard"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("sample-service-2"));
    }
}