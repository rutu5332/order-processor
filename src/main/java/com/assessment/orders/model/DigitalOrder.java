package com.assessment.orders.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * digital order needs a download link.
 */
public record DigitalOrder(
        String orderId,
        String customerId,
        double amount,
        String downloadLink
) implements OrderEvent {

    @JsonCreator
    public DigitalOrder(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("amount") double amount,
            @JsonProperty("downloadLink") String downloadLink
    ) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.downloadLink = downloadLink;
    }
}
