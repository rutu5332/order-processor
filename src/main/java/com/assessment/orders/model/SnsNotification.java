package com.assessment.orders.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the SNS notification envelope that wraps the order payload
 * when SNS delivers to SQS with raw message delivery disabled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SnsNotification(
        @JsonProperty("Type") String type,
        @JsonProperty("Message") String message
) {
}
