package com.aiproject.scaffold.demo.messaging;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.integration.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RocketMQMessageListener(
        topic = "${app.integration.rocketmq.demo-topic}",
        consumerGroup = "${rocketmq.consumer.group}"
)
public class DemoMessageListener implements RocketMQListener<String> {
    private static final Logger log = LoggerFactory.getLogger(DemoMessageListener.class);

    @Override
    public void onMessage(String message) {
        log.info("Received demo RocketMQ message: {}", message);
    }
}
