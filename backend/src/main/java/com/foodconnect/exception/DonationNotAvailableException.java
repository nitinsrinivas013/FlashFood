package com.foodconnect.exception;

/**
 * Thrown when a donation is not in a state that allows the requested action
 * (e.g., trying to claim an already-claimed donation).
 */
public class DonationNotAvailableException extends RuntimeException {

    public DonationNotAvailableException(String message) {
        super(message);
    }

    public DonationNotAvailableException(Long donationId) {
        super(String.format("Donation with ID %d is not available for this action", donationId));
    }
}
