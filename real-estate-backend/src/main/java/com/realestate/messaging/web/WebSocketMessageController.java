package com.realestate.messaging.web;

import com.realestate.messaging.dto.MessageDto;
import com.realestate.messaging.service.MessagingService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketMessageController {

    private final MessagingService messagingService;

    public WebSocketMessageController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @MessageMapping("/conversations/{conversationId}")
    @SendTo("/topic/conversations/{conversationId}")
    public MessageDto sendMessage(
            @DestinationVariable Long conversationId,
            Map<String, Object> payload) {

        String content = String.valueOf(payload.get("content"));

        Long currentUserId = Long.valueOf(
                String.valueOf(payload.get("senderId"))
        );

        return messagingService.sendMessage(
                conversationId,
                content,
                currentUserId
        );
    }
}