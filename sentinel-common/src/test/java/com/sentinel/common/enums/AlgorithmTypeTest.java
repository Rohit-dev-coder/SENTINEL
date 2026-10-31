package com.sentinel.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlgorithmTypeTest {

    @Test
    void testAlgorithmTypeValues() {
        assertEquals(4, AlgorithmType.values().length);
        assertEquals(AlgorithmType.TOKEN_BUCKET, AlgorithmType.valueOf("TOKEN_BUCKET"));
        assertEquals(AlgorithmType.SLIDING_WINDOW_LOG, AlgorithmType.valueOf("SLIDING_WINDOW_LOG"));
        assertEquals(AlgorithmType.FIXED_WINDOW_COUNTER, AlgorithmType.valueOf("FIXED_WINDOW_COUNTER"));
        assertEquals(AlgorithmType.SLIDING_WINDOW_COUNTER, AlgorithmType.valueOf("SLIDING_WINDOW_COUNTER"));
    }

    @Test
    void testGetDisplayName() {
        assertEquals("Token Bucket", AlgorithmType.TOKEN_BUCKET.getDisplayName());
        assertEquals("Sliding Window Log", AlgorithmType.SLIDING_WINDOW_LOG.getDisplayName());
        assertEquals("Fixed Window Counter", AlgorithmType.FIXED_WINDOW_COUNTER.getDisplayName());
        assertEquals("Sliding Window Counter", AlgorithmType.SLIDING_WINDOW_COUNTER.getDisplayName());
    }

    @Test
    void testFromStringValid() {
        assertEquals(AlgorithmType.TOKEN_BUCKET, AlgorithmType.fromString("TOKEN_BUCKET"));
        assertEquals(AlgorithmType.TOKEN_BUCKET, AlgorithmType.fromString("token_bucket"));
        assertEquals(AlgorithmType.SLIDING_WINDOW_LOG, AlgorithmType.fromString("SLIDING_WINDOW_LOG"));
        assertEquals(AlgorithmType.FIXED_WINDOW_COUNTER, AlgorithmType.fromString("FIXED_WINDOW_COUNTER"));
        assertEquals(AlgorithmType.SLIDING_WINDOW_COUNTER, AlgorithmType.fromString("SLIDING_WINDOW_COUNTER"));
    }

    @Test
    void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AlgorithmType.fromString("INVALID_ALGORITHM"));
        assertThrows(IllegalArgumentException.class, () -> AlgorithmType.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> AlgorithmType.fromString(null));
    }
}