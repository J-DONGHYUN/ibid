package project.kjhjdh.ibid.chat.presentation.dto;

import project.kjhjdh.ibid.chat.domain.ChatRoom;

public record ChatRoomResponse(
        Long chatRoomId,
        Long productId,
        Long sellerId,
        Long buyerId
) {

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getProductId(),
                chatRoom.getSellerId(),
                chatRoom.getBuyerId()
        );
    }
}
