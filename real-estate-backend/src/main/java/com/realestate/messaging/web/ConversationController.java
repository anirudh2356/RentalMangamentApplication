package com.realestate.messaging.web;

import com.realestate.messaging.dto.ConversationSummaryDto;
import com.realestate.messaging.dto.MessageDto;
import com.realestate.messaging.dto.CreateConversationRequest;
import com.realestate.messaging.dto.SendMessageRequest;
import com.realestate.messaging.service.MessagingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173"
})
public class ConversationController {

    private final MessagingService messagingService;

    public ConversationController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping
    public List<ConversationSummaryDto> listConversations(
            @RequestHeader("X-User-Id") Long currentUserId) {
        return messagingService.listConversations(currentUserId);
    }

    @PostMapping
    public ConversationSummaryDto createConversation(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestBody CreateConversationRequest request) {

        return messagingService.createConversation(
                currentUserId,
                request.otherUserId()
        );
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> getMessages(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long currentUserId) {

        return messagingService.getMessages(id, currentUserId);
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markConversationAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long currentUserId) {

        messagingService.markConversationAsRead(id, currentUserId);
    }

    @PostMapping("/{id}/messages")
    public MessageDto sendMessage(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestBody SendMessageRequest request) {

        return messagingService.sendMessage(
                id,
                request.content(),
                currentUserId
        );
    }

    @PutMapping("/{conversationId}/messages/{messageId}")
    public MessageDto editMessage(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestBody SendMessageRequest request) {

        return messagingService.editMessage(
                conversationId,
                messageId,
                request.content(),
                currentUserId
        );
    }

    @DeleteMapping("/{conversationId}/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long currentUserId) {

        messagingService.deleteMessage(
                conversationId,
                messageId,
                currentUserId
        );
    }
}