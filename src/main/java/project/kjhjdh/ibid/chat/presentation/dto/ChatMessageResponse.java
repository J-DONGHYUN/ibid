package project.kjhjdh.ibid.chat.presentation.dto;

import java.time.LocalDateTime;

import project.kjhjdh.ibid.chat.domain.ChatMessage;

public record ChatMessageResponse(
        Long messageId,
        Long chatRoomId,
        Long senderId,
        String content,
        String clientMessageId,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                message.getContent(),
                message.getClientMessageId(),
                message.getCreatedAt()
        );
    }
}
