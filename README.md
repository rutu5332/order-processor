# Event-Driven Order Processing - AWS Lambda

A Lambda function that reads "Order" messages from SQS (which come from SNS),
figures out what type of order each one is, processes it, and sends a
confirmation back to SNS.

## How it works

```
SNS topic → SQS queue → Lambda reads each message
                          → unwraps the SNS envelope
                          → parses it into a DigitalOrder or PhysicalOrder
                          → logs what it's doing
                          → publishes "Order Processed" to SNS
```

The one tricky part: SQS's message body isn't the order itself — it's an
SNS envelope, and the real order JSON is nested inside it as a string in
the `Message` field. That's why parsing happens twice.

## Project structure

```
order-processor/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/assessment/orders/
    │   ├── OrderProcessorLambda.java   → the Lambda handler
    │   ├── LocalDemoRunner.java        → run this to see it work, no AWS needed
    │   └── model/
    │       ├── OrderEvent.java         → base type (sealed interface)
    │       ├── DigitalOrder.java
    │       ├── PhysicalOrder.java
    │       └── SnsNotification.java    → models the SNS envelope
    └── test/java/com/assessment/orders/
        └── OrderProcessorLambdaTest.java  → automated tests
```

## Setup (one-time)

```bash
brew install openjdk@21 maven
```

## Build & test

```bash
cd order-processor
mvn clean test
```

You should see `BUILD SUCCESS` and `Tests run: 3, Failures: 0`.

## Run it and watch it work

```bash
mvn compile exec:java
```

This feeds it 3 sample messages (a digital order, a physical order, and
one broken message) and prints what happens at each step.

## Package for deployment

```bash
mvn clean package
```

Produces `target/order-processor.jar` — this is what you would upload to a
real AWS Lambda function.
