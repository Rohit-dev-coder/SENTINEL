package com.sentinel.limiter;

public class RateLimitResult {
    
    private final boolean allowed;
    private final int remaining;
    private final long resetTime;
    
    public RateLimitResult(boolean allowed, int remaining, long resetTime) {
        this.allowed = allowed;
        this.remaining = remaining;
        this.resetTime = resetTime;
    }
    
    public boolean isAllowed() {
        return allowed;
    }
    
    public int getRemaining() {
        return remaining;
    }
    
    public long getResetTime() {
        return resetTime;
    }
    
    @Override
    public String toString() {
        return "RateLimitResult{" +
                "allowed=" + allowed +
                ", remaining=" + remaining +
                ", resetTime=" + resetTime +
                '}';
    }
}