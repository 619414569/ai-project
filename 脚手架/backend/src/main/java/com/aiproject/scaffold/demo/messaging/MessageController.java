package com.aiproject.scaffold.demo.messaging;

import com.aiproject.scaffold.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo/messages")
public class MessageController {
    private final MessagePublisher messagePublisher;

    public MessageController(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @PostMapping
    public ApiResponse<MessagePublisher.MessageReceipt> publish(@Valid @RequestBody MessageRequest request) {
        return ApiResponse.success(messagePublisher.publish(request.body()));
    }

    public record MessageRequest(
            @NotBlank(message = "Message body is required")
            @Size(max = 4096, message = "Message body must not exceed 4096 characters")
            String body
    ) {
    }
}
