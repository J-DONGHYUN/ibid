package project.kjhjdh.ibid.chat.infra;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.chat.domain.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByChatRoomIdAndClientMessageId(Long chatRoomId, String clientMessageId);
}
