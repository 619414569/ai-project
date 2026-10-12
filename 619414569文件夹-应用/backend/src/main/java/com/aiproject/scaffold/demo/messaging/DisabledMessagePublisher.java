package com.aiproject.scaffold.demo.messaging;

import com.aiproject.scaffold.common.api.ErrorCode;
import com.aiproject.scaffold.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.integration.rocketmq", name = "enabled", havingValue = "false")
public class DisabledMessagePublisher implements MessagePublisher {
    @Override
    public MessageReceipt publish(String body) {
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "RocketMQ integration is disabled");
    }
}
