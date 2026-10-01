package project.kjhjdh.ibid.chat.infra;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.kjhjdh.ibid.chat.domain.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);

    Slice<ChatRoom> findByProductIdAndSellerIdAndIdLessThanOrderByIdDesc(
            Long productId, Long sellerId, Long id, Pageable pageable);

    @Query("select r.id as roomId, max(m.id) as lastMessageId "
            + "from ChatRoom r join ChatMessage m on m.chatRoomId = r.id "
            + "where r.sellerId = :userId or r.buyerId = :userId "
            + "group by r.id "
            + "having max(m.id) < :cursor "
            + "order by max(m.id) desc")
    Slice<RoomLastMessage> findMyRoomsOrderByLastMessageDesc(
            @Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);
}
