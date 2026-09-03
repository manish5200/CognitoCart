package com.manish.smartcart.shared.exception;

/**
 * Thrown when an invalid lifecycle or approval status transition is attempted.
 * Edge Cases Handled:
 * - Attempting to publish an item that hasn't passed moderation (DRAFT -> ACTIVE while PENDING_REVIEW)
 * - Attempting to re-submit a permanently banned/archived item.
 * <p>
 * Note: We do NOT use @ResponseStatus here because the GlobalExceptionHandler takes precedence.
 */

public class ProductStateTransitionException extends RuntimeException{
    public ProductStateTransitionException(String message) {
        super(message);
    }
}
