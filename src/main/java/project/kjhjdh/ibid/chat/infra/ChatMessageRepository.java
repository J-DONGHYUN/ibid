package project.kjhjdh.ibid.chat.infra;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.chat.domain.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByChatRoomIdAndClientMessageId(Long chatRoomId, String clientMessageId);

    Slice<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long chatRoomId, Long id, Pageable pageable);

    long countByChatRoomIdAndIdGreaterThanAndSenderIdNot(Long chatRoomId, Long id, Long senderId);
}
