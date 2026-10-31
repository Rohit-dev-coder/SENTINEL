package com.sentinel.common.exception;

/**
 * Exception thrown when rate limit is exceeded.
 */
public class RateLimitExceededException extends RuntimeException {
    private final int limit;
    private final int remaining;
    private final long resetAt;

    public RateLimitExceededException(String message, int limit, int remaining, long resetAt) {
        super(message);
        this.limit = limit;
        this.remaining = remaining;
        this.resetAt = resetAt;
    }

    public int getLimit() {
        return limit;
    }

    public int getRemaining() {
        return remaining;
    }

    public long getResetAt() {
        return resetAt;
    }
}