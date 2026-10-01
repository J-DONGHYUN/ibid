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
        name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_rooms_product_buyer", columnNames = {"product_id", "buyer_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ChatRoom(Long productId, Long sellerId, Long buyerId) {
        if (sellerId.equals(buyerId)) {
            throw new BusinessException(ErrorCode.CANNOT_OPEN_CHAT_ON_OWN_PRODUCT);
        }
        this.productId = productId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.createdAt = LocalDateTime.now();
    }

    public static ChatRoom open(Long productId, Long sellerId, Long buyerId) {
        return new ChatRoom(productId, sellerId, buyerId);
    }

    public boolean isParticipant(Long userId) {
        return sellerId.equals(userId) || buyerId.equals(userId);
    }
}
