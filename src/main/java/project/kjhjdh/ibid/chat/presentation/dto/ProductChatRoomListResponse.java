package project.kjhjdh.ibid.chat.presentation.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.chat.application.ProductChatRoomResult;

public record ProductChatRoomListResponse(
        List<Item> rooms,
        Long nextCursor,
        boolean hasNext
) {

    public record Item(Long chatRoomId, Long buyerId, ChatMessageResponse lastMessage, long unreadCount) {

        static Item from(ProductChatRoomResult result) {
            return new Item(
                    result.chatRoomId(),
                    result.buyerId(),
                    result.lastMessage() == null ? null : ChatMessageResponse.from(result.lastMessage()),
                    result.unreadCount());
        }
    }

    public static ProductChatRoomListResponse of(Slice<ProductChatRoomResult> slice) {
        List<ProductChatRoomResult> content = slice.getContent();
        Long nextCursor = slice.hasNext() && !content.isEmpty()
                ? content.get(content.size() - 1).chatRoomId()
                : null;
        return new ProductChatRoomListResponse(
                content.stream().map(Item::from).toList(),
                nextCursor,
                slice.hasNext());
    }
}
