package com.sentinel.common.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TierTest {

    @Test
    void testTierValues() {
        assertEquals(4, Tier.values().length);
        assertEquals(Tier.FREE, Tier.valueOf("FREE"));
        assertEquals(Tier.BASIC, Tier.valueOf("BASIC"));
        assertEquals(Tier.PREMIUM, Tier.valueOf("PREMIUM"));
        assertEquals(Tier.ENTERPRISE, Tier.valueOf("ENTERPRISE"));
    }

    @Test
    void testDefaultLimits() {
        assertEquals(10, Tier.FREE.getDefaultMaxRequests());
        assertEquals(60, Tier.FREE.getDefaultWindowSeconds());
        
        assertEquals(100, Tier.BASIC.getDefaultMaxRequests());
        assertEquals(60, Tier.BASIC.getDefaultWindowSeconds());
        
        assertEquals(1000, Tier.PREMIUM.getDefaultMaxRequests());
        assertEquals(60, Tier.PREMIUM.getDefaultWindowSeconds());
        
        assertEquals(10000, Tier.ENTERPRISE.getDefaultMaxRequests());
        assertEquals(60, Tier.ENTERPRISE.getDefaultWindowSeconds());
    }

    @Test
    void testFromStringValid() {
        assertEquals(Tier.FREE, Tier.fromString("FREE"));
        assertEquals(Tier.FREE, Tier.fromString("free"));
        assertEquals(Tier.BASIC, Tier.fromString("BASIC"));
        assertEquals(Tier.PREMIUM, Tier.fromString("PREMIUM"));
        assertEquals(Tier.ENTERPRISE, Tier.fromString("ENTERPRISE"));
    }

    @Test
    void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Tier.fromString("INVALID_TIER"));
        assertThrows(IllegalArgumentException.class, () -> Tier.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> Tier.fromString(null));
    }
}