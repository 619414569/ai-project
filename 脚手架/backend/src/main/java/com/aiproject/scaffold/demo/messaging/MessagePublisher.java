package com.aiproject.scaffold.demo.messaging;

public interface MessagePublisher {
    MessageReceipt publish(String body);

    record MessageReceipt(String messageId, String destination) {
    }
}
