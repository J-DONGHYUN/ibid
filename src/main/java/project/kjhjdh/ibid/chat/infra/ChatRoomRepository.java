package project.kjhjdh.ibid.chat.infra;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.chat.domain.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);
}
