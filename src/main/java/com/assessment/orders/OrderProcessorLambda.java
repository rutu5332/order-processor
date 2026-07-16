package com.assessment.orders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.assessment.orders.model.DigitalOrder;
import com.assessment.orders.model.OrderEvent;
import com.assessment.orders.model.PhysicalOrder;
import com.assessment.orders.model.SnsNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Processes order events delivered via SNS -> SQS -> Lambda, and
 * publishes a confirmation back to SNS once each order is handled.
 */
public class OrderProcessorLambda implements RequestHandler<SQSEvent, String> {

    private static final String ORDER_PROCESSED_TOPIC_ARN =
            "arn:aws:sns:us-east-1:123456789012:OrderProcessedTopic";

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    public OrderProcessorLambda() {
        // Initialize the SNS client
        this.snsClient = createSnsClient();
        this.objectMapper = new ObjectMapper();
    }

    protected SnsClient createSnsClient() {
        return SnsClient.create();
    }

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (SQSEvent.SQSMessage sqsMessage : sqsEvent.getRecords()) {
                executor.submit(() -> processMessage(sqsMessage, context));
            }
        }

        return "Processed " + sqsEvent.getRecords().size() + " messages.";
    }

    private void processMessage(SQSEvent.SQSMessage sqsMessage, Context context) {
        try {

            OrderEvent order = parseOrderEvent(sqsMessage.getBody());

            switch (order) {
                case DigitalOrder d -> context.getLogger().log(
                        "Processing DIGITAL order " + d.orderId()
                                + " for customer " + d.customerId()
                                + " -> delivering via " + d.downloadLink());
                case PhysicalOrder p -> context.getLogger().log(
                        "Processing PHYSICAL order " + p.orderId()
                                + " for customer " + p.customerId()
                                + " -> shipping to " + p.shippingAddress());
            }

            publishOrderProcessed();

        } catch (Exception e) {

            context.getLogger().log("Failed to process message "
                    + sqsMessage.getMessageId() + ": " + e.getMessage());
        }
    }

    /**
     * Unwraps the SNS envelope from the SQS message body and deserializes
     * the nested order payload into an {@link OrderEvent}.
     */
    private OrderEvent parseOrderEvent(String sqsBody) throws Exception {
        JsonNode root = objectMapper.readTree(sqsBody);
        SnsNotification notification = objectMapper.treeToValue(root, SnsNotification.class);
        return objectMapper.readValue(notification.message(), OrderEvent.class);
    }

    protected void publishOrderProcessed() {
        PublishRequest request = PublishRequest.builder()
                .topicArn(ORDER_PROCESSED_TOPIC_ARN)
                .message("Order Processed")
                .build();

        snsClient.publish(request);
    }
}