package com.assessment.orders.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base type for all order events.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "orderType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DigitalOrder.class, name = "DIGITAL"),
        @JsonSubTypes.Type(value = PhysicalOrder.class, name = "PHYSICAL")
})
public sealed interface OrderEvent permits DigitalOrder, PhysicalOrder {
    String orderId();
    String customerId();
    double amount();
}
