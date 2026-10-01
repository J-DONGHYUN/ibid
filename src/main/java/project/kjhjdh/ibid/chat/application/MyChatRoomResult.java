package project.kjhjdh.ibid.chat.application;

import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.product.application.ProductSummary;

public record MyChatRoomResult(
        Long chatRoomId,
        Long peerId,
        ProductSummary product,
        ChatMessage lastMessage,
        long unreadCount
) {
}
