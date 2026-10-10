package com.aiproject.scaffold.demo.messaging;

import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;
import com.aiproject.scaffold.common.util.IdUtils;
import com.aiproject.scaffold.config.IntegrationProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.integration.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketMqMessagePublisher implements MessagePublisher {
    private static final Logger log = LoggerFactory.getLogger(RocketMqMessagePublisher.class);
    private final RocketMQTemplate rocketMQTemplate;
    private final IntegrationProperties properties;

    public RocketMqMessagePublisher(RocketMQTemplate rocketMQTemplate, IntegrationProperties properties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.properties = properties;
    }

    @Override
    public MessageReceipt publish(String body) {
        String messageId = IdUtils.compactUuid();
        String payload = messageId + ":" + body;
        try {
            rocketMQTemplate.convertAndSend(properties.rocketmq().demoTopic(), payload);
        } catch (RuntimeException exception) {
            log.error("Unable to publish RocketMQ message {}", messageId, exception);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "RocketMQ publish failed");
        }
        return new MessageReceipt(messageId, properties.rocketmq().demoTopic());
    }
}
