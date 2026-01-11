package com.hotelmanagement.quanlikhachsan.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorDefinition {
    // Room errors
    ROOM_NOT_FOUND("Room not found.", HttpStatus.NOT_FOUND, "ROOM_001"),
    DUPLICATE_ID("Duplicate ID.", HttpStatus.CONFLICT, "ROOM_002"),
    ROOM_IN_USE("Cannot delete room that is currently occupied or reserved.", HttpStatus.CONFLICT, "ROOM_003"),
    ROOM_IMAGE_NOT_FOUND("Room image not found.", HttpStatus.NOT_FOUND, "ROOM_004"),
    ROOM_IMAGE_UPLOAD_FAILED("Failed to upload room image.", HttpStatus.INTERNAL_SERVER_ERROR, "ROOM_005"),
    ROOM_IMAGE_DELETE_FAILED("Failed to delete room image.", HttpStatus.INTERNAL_SERVER_ERROR, "ROOM_006"),
    ROOM_TYPE_NOT_FOUND("Room type not found.", HttpStatus.NOT_FOUND, "ROOM_007"),
    ROOM_STATUS_NOT_FOUND("Room status not found.", HttpStatus.NOT_FOUND, "ROOM_008"),

    // Reservation errors
    RESERVATION_NOT_FOUND("Reservation not found.", HttpStatus.NOT_FOUND, "RESERVATION_001"),
    ROOM_NOT_AVAILABLE("One or more rooms are not available for the selected dates.", HttpStatus.CONFLICT,
            "RESERVATION_002"),
    INVALID_DATE_RANGE("Check-out date must be after check-in date.", HttpStatus.BAD_REQUEST, "RESERVATION_003"),
    GUEST_NOT_FOUND("Guest not found.", HttpStatus.NOT_FOUND, "RESERVATION_004"),
    RESERVATION_ALREADY_CANCELLED("Reservation is already cancelled.", HttpStatus.CONFLICT, "RESERVATION_005"),
    RESERVATION_ALREADY_CHECKED_IN("Reservation is already checked in.", HttpStatus.CONFLICT, "RESERVATION_006"),
    RESERVATION_CANNOT_MODIFY("Cannot modify a reservation that is checked out or cancelled.", HttpStatus.CONFLICT,
            "RESERVATION_007"),
    PAST_CHECK_IN_DATE("Check-in date cannot be in the past.", HttpStatus.BAD_REQUEST, "RESERVATION_008"),

    // Payment errors
    PAYMENT_NOT_FOUND("Payment not found.", HttpStatus.NOT_FOUND, "PAYMENT_001"),
    PAYMENT_FAILED("Payment processing failed. Please try again or use a different payment method.",
            HttpStatus.BAD_REQUEST, "PAYMENT_002"),
    PAYMENT_AMOUNT_INVALID("Payment amount must be positive and within acceptable limits.", HttpStatus.BAD_REQUEST,
            "PAYMENT_003"),
    PAYMENT_STRIPE_ERROR("Payment processing error: ", HttpStatus.BAD_REQUEST, "PAYMENT_004"),
    PAYMENT_RATE_LIMITED("Too many payment attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS,
            "PAYMENT_005"),
    PAYMENT_WEBHOOK_ERROR("Webhook processing failed.", HttpStatus.BAD_REQUEST, "PAYMENT_006"),
    PAYMENT_SESSION_EXPIRED("Payment session has expired. Please initiate a new payment.",
            HttpStatus.GONE, "PAYMENT_007");

    private final String message;
    private final HttpStatus statusCode;
    private final String errorCode;

    ErrorDefinition(String message, HttpStatus statusCode, String errorCode) {
        this.message = message;
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public AppError toAppError() {
        return AppError.from(this.message, this.statusCode, this.errorCode);
    }
}
