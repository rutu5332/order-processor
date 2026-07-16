package com.assessment.orders;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderProcessorLambdaTest {

    @Test
    void digitalOrder_isProcessedAndPublished() {
        String digitalOrderJson = """
                {"orderType":"DIGITAL","orderId":"D-1001","customerId":"cust-1","amount":19.99,"downloadLink":"https://cdn.example.com/d/1001"}""";

        RecordingOrderProcessorLambda handler = new RecordingOrderProcessorLambda();
        String result = handler.handleRequest(
                eventWithOneMessage("sqs-1", wrapInSnsEnvelope(digitalOrderJson)), new FakeContext());

        assertEquals("Processed 1 messages.", result);
        assertEquals(1, handler.publishCount.get());
    }

    @Test
    void physicalOrder_isProcessedAndPublished() {
        String physicalOrderJson = """
                {"orderType":"PHYSICAL","orderId":"P-2001","customerId":"cust-2","amount":54.50,"shippingAddress":"221B Baker Street"}""";

        RecordingOrderProcessorLambda handler = new RecordingOrderProcessorLambda();
        String result = handler.handleRequest(
                eventWithOneMessage("sqs-2", wrapInSnsEnvelope(physicalOrderJson)), new FakeContext());

        assertEquals("Processed 1 messages.", result);
        assertEquals(1, handler.publishCount.get());
    }

    @Test
    void malformedMessage_doesNotCrashTheWholeBatch() {
        String digitalOrderJson = """
                {"orderType":"DIGITAL","orderId":"D-1001","customerId":"cust-1","amount":19.99,"downloadLink":"https://cdn.example.com/d/1001"}""";

        SQSEvent.SQSMessage goodMsg = sqsMessage("sqs-good", wrapInSnsEnvelope(digitalOrderJson));
        SQSEvent.SQSMessage badMsg = sqsMessage("sqs-bad", "not valid json");

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(goodMsg, badMsg));

        RecordingOrderProcessorLambda handler = new RecordingOrderProcessorLambda();
        String result = handler.handleRequest(event, new FakeContext());

        assertEquals("Processed 2 messages.", result);
        assertEquals(1, handler.publishCount.get());
    }

    // ---- helpers ----

    private static String wrapInSnsEnvelope(String orderJson) {
        String escaped = orderJson.replace("\"", "\\\"");
        return """
                {"Type":"Notification","MessageId":"abc-123","TopicArn":"arn:aws:sns:us-east-1:123456789012:OrdersTopic","Message":"%s","Timestamp":"2026-07-15T00:00:00.000Z"}"""
                .formatted(escaped);
    }

    private static SQSEvent.SQSMessage sqsMessage(String messageId, String body) {
        SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
        msg.setMessageId(messageId);
        msg.setBody(body);
        return msg;
    }

    private static SQSEvent eventWithOneMessage(String messageId, String body) {
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(sqsMessage(messageId, body)));
        return event;
    }

    /** Fake Context so we don't need a real Lambda runtime to test. */
    static class FakeContext implements Context {
        public String getAwsRequestId() { return "test-request-id"; }
        public String getLogGroupName() { return "test-log-group"; }
        public String getLogStreamName() { return "test-log-stream"; }
        public String getFunctionName() { return "test-function"; }
        public String getFunctionVersion() { return "1"; }
        public String getInvokedFunctionArn() { return "arn:aws:lambda:us-east-1:123456789012:function:test"; }
        public CognitoIdentity getIdentity() { return null; }
        public ClientContext getClientContext() { return null; }
        public int getRemainingTimeInMillis() { return 30000; }
        public int getMemoryLimitInMB() { return 512; }

        public LambdaLogger getLogger() {
            return new LambdaLogger() {
                @Override
                public void log(String message) {
                    System.out.println("[LAMBDA LOG] " + message);
                }

                @Override
                public void log(byte[] message) {
                    System.out.println("[LAMBDA LOG] " + new String(message));
                }
            };
        }
    }

    /** Test double that avoids real AWS calls. */
    static class RecordingOrderProcessorLambda extends OrderProcessorLambda {
        final AtomicInteger publishCount = new AtomicInteger(0);

        @Override
        protected software.amazon.awssdk.services.sns.SnsClient createSnsClient() {
            return null; // avoid needing real AWS credentials in this sandbox
        }

        @Override
        protected void publishOrderProcessed() {
            publishCount.incrementAndGet();
        }
    }
}