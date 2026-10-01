package project.kjhjdh.ibid.chat.application;

public record SendMessageCommand(
        Long chatRoomId,
        Long senderId,
        String clientMessageId,
        String content
) {
}
