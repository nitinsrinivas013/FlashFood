package com.foodconnect.exception;

/**
 * Thrown when an action conflicts with the current state of a resource.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
