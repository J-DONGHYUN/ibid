package project.kjhjdh.ibid.chat.presentation.dto;

import java.time.LocalDateTime;

import project.kjhjdh.ibid.chat.application.MyChatRoomResult;
import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.product.application.ProductSummary;

public record MyChatRoomResponse(
        Long chatRoomId,
        Long peerId,
        ProductBrief product,
        LastMessageBrief lastMessage,
        long unreadCount
) {

    public record ProductBrief(Long productId, String title, String thumbnailUrl) {

        static ProductBrief from(ProductSummary summary) {
            if (summary == null) {
                return null;
            }
            return new ProductBrief(summary.productId(), summary.title(), summary.thumbnailUrl());
        }
    }

    public record LastMessageBrief(Long messageId, Long senderId, String content, LocalDateTime createdAt) {

        static LastMessageBrief from(ChatMessage message) {
            if (message == null) {
                return null;
            }
            return new LastMessageBrief(
                    message.getId(), message.getSenderId(), message.getContent(), message.getCreatedAt());
        }
    }

    public static MyChatRoomResponse from(MyChatRoomResult result) {
        return new MyChatRoomResponse(
                result.chatRoomId(),
                result.peerId(),
                ProductBrief.from(result.product()),
                LastMessageBrief.from(result.lastMessage()),
                result.unreadCount());
    }
}
