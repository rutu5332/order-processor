package com.assessment.orders.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * physical order needs a shipping address.
 */
public record PhysicalOrder(
        String orderId,
        String customerId,
        double amount,
        String shippingAddress
) implements OrderEvent {

    @JsonCreator
    public PhysicalOrder(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("amount") double amount,
            @JsonProperty("shippingAddress") String shippingAddress
    ) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.shippingAddress = shippingAddress;
    }
}
