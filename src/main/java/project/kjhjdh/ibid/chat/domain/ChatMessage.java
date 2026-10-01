package project.kjhjdh.ibid.chat.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@Getter
@Entity
@Table(
        name = "chat_messages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_messages_room_client_id",
                columnNames = {"chat_room_id", "client_message_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    private static final int CONTENT_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    @Column(name = "client_message_id", nullable = false)
    private String clientMessageId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ChatMessage(Long chatRoomId, Long senderId, String content, String clientMessageId) {
        validateContent(content);
        if (clientMessageId == null || clientMessageId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_MESSAGE);
        }
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.content = content;
        this.clientMessageId = clientMessageId;
        this.createdAt = LocalDateTime.now();
    }

    public static ChatMessage create(Long chatRoomId, Long senderId, String content, String clientMessageId) {
        return new ChatMessage(chatRoomId, senderId, content, clientMessageId);
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > CONTENT_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_MESSAGE_CONTENT);
        }
    }
}
