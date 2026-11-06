package com.foodconnect.exception;

/**
 * Thrown when a user attempts an action they are not authorized to perform.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
