package project.kjhjdh.ibid.chat.application;

import project.kjhjdh.ibid.chat.domain.ChatMessage;

public record ProductChatRoomResult(
        Long chatRoomId,
        Long buyerId,
        ChatMessage lastMessage,
        long unreadCount
) {
}
