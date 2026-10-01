package project.kjhjdh.ibid.chat.application;

public record ReadReceiptResult(
        Long chatRoomId,
        Long readerId,
        Long lastReadMessageId
) {
}
