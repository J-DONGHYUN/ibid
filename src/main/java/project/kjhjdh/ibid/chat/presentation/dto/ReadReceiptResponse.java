package project.kjhjdh.ibid.chat.presentation.dto;

import project.kjhjdh.ibid.chat.application.ReadReceiptResult;

public record ReadReceiptResponse(
        Long chatRoomId,
        Long readerId,
        Long lastReadMessageId
) {

    public static ReadReceiptResponse from(ReadReceiptResult result) {
        return new ReadReceiptResponse(
                result.chatRoomId(), result.readerId(), result.lastReadMessageId());
    }
}
