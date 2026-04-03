package io.cb_demos.ecommerce.exception;

import io.cb_demos.ecommerce.domain.Order;

/**
 * Exception thrown when attempting an invalid order state transition.
 * For example, trying to cancel an order that has already been shipped.
 */
public class InvalidOrderStateException extends RuntimeException {

    private final Long orderId;
    private final Order.OrderStatus currentStatus;

    public InvalidOrderStateException(Long orderId, Order.OrderStatus currentStatus, String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public InvalidOrderStateException(String message) {
        super(message);
        this.orderId = null;
        this.currentStatus = null;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Order.OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
