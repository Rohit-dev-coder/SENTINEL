package com.sentinel.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "mySecretKeyForSentinelGatewayThatIsLongEnoughForHS256");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void testGenerateToken() {
        String apiKey = "test_api_key_123";
        String token = jwtUtil.generateToken(apiKey);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractApiKey() {
        String apiKey = "test_api_key_456";
        String token = jwtUtil.generateToken(apiKey);
        
        String extractedApiKey = jwtUtil.extractApiKey(token);
        
        assertEquals(apiKey, extractedApiKey);
    }

    @Test
    void testValidateValidToken() {
        String apiKey = "test_api_key_789";
        String token = jwtUtil.generateToken(apiKey);
        
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void testValidateEmptyToken() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void testValidateNullToken() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void testTokenExtractionConsistency() {
        String apiKey = "test_api_key_consistency";
        String token = jwtUtil.generateToken(apiKey);
        
        String extractedApiKey = jwtUtil.extractApiKey(token);
        
        assertEquals(apiKey, extractedApiKey);
        assertTrue(jwtUtil.validateToken(token));
    }
}