package com.sentinel.admin.exception;

/**
 * Thrown when a tenant or rule does not exist. Extends IllegalArgumentException so existing
 * callers that catch it keep working; the web layer maps it to HTTP 404.
 */
public class ResourceNotFoundException extends IllegalArgumentException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
