package com.assessment.orders;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.services.sns.SnsClient;

import java.util.List;

/**
 * Runnable entry point for manually running the whole pipeline execute,
 * outside of a test framework. Not part of the actual Lambda deployment
 */
public class LocalDemoRunner {

    public static void main(String[] args) {
        System.out.println("=== Building a sample batch of 3 SQS messages ===");

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(
                sqsMessage("sqs-1", wrapInSnsEnvelope(digitalOrderJson())),
                sqsMessage("sqs-2", wrapInSnsEnvelope(physicalOrderJson())),
                sqsMessage("sqs-3", "not valid json at all")   // proves error handling doesn't crash the batch
        ));

        System.out.println("=== Invoking the handler ===");
        OrderProcessorLambda handler = new LocalOrderProcessorLambda();
        String result = handler.handleRequest(event, new PrintingContext());

        System.out.println("=== Handler returned ===");
        System.out.println(result);
    }

    private static String digitalOrderJson() {
        return """
                {"orderType":"DIGITAL","orderId":"D-1001","customerId":"cust-1","amount":19.99,"downloadLink":"https://cdn.example.com/d/1001"}""";
    }

    private static String physicalOrderJson() {
        return """
                {"orderType":"PHYSICAL","orderId":"P-2001","customerId":"cust-2","amount":54.50,"shippingAddress":"221B Baker Street"}""";
    }

    /** Wraps a raw order JSON string as an SNS notification envelope, matching real SNS->SQS delivery shape. */
    private static String wrapInSnsEnvelope(String orderJson) {
        String escaped = orderJson.replace("\"", "\\\"");
        return """
                {"Type":"Notification","MessageId":"msg-id","TopicArn":"arn:aws:sns:us-east-1:123456789012:OrdersTopic","Message":"%s","Timestamp":"2026-07-15T00:00:00.000Z"}"""
                .formatted(escaped);
    }

    private static SQSEvent.SQSMessage sqsMessage(String messageId, String body) {
        SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
        msg.setMessageId(messageId);
        msg.setBody(body);
        return msg;
    }

    static class LocalOrderProcessorLambda extends OrderProcessorLambda {
        @Override
        protected SnsClient createSnsClient() {
            return null;
        }

        @Override
        protected void publishOrderProcessed() {
            System.out.println("[WOULD PUBLISH TO SNS] Order Processed");
        }
    }

    /** Minimal Context implementation so the handler can run outside a real Lambda runtime. */
    static class PrintingContext implements Context {
        public String getAwsRequestId() { return "local-request-id"; }
        public String getLogGroupName() { return "local-log-group"; }
        public String getLogStreamName() { return "local-log-stream"; }
        public String getFunctionName() { return "local-function"; }
        public String getFunctionVersion() { return "1"; }
        public String getInvokedFunctionArn() { return "arn:aws:lambda:us-east-1:123456789012:function:local"; }
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
}