package project.kjhjdh.ibid.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.notification.domain.NotificationType;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;
import project.kjhjdh.ibid.trade.application.CompletionService;
import project.kjhjdh.ibid.trade.application.ReservationService;

/**
 * 거래 유스케이스에서 알림 행까지를 끊지 않고 잇는다.
 * 예약 · 거래완료는 트랜잭션 안이라 리스너가 커밋 후에 돌아야 한다 (ADR-0014).
 */
class TradeNotificationFlowTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CompletionService completionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @DisplayName("[NT-02] 예약하면 예약 상대에게 알림 행이 생긴다 (커밋 후)")
    @Test
    void reserveStoresNotification() {
        // given
        Long productId = openRoomOnProduct();

        // when
        reservationService.reserve(productId, SELLER_ID, BUYER_ID);

        // then
        assertThat(notificationRepository.findAll()).singleElement().satisfies(n -> {
            assertThat(n.getRecipientId()).isEqualTo(BUYER_ID);
            assertThat(n.getType()).isEqualTo(NotificationType.RESERVED);
            assertThat(n.getReferenceId()).isEqualTo(productId);
        });
    }

    @DisplayName("[NT-02] 예약을 해제하면 예약 상대에게 알림 행이 생긴다 (커밋 후)")
    @Test
    void cancelReservationStoresNotification() {
        // given
        Long productId = openRoomOnProduct();
        reservationService.reserve(productId, SELLER_ID, BUYER_ID);

        // when
        reservationService.cancelReservation(productId, SELLER_ID);

        // then
        assertThat(notificationRepository.findAll())
                .anySatisfy(n -> {
                    assertThat(n.getRecipientId()).isEqualTo(BUYER_ID);
                    assertThat(n.getType()).isEqualTo(NotificationType.RESERVATION_CANCELED);
                });
    }

    @DisplayName("[NT-02] 거래완료하면 거래 상대에게 알림 행이 생긴다 (커밋 후)")
    @Test
    void completeStoresNotification() {
        // given
        Long productId = openRoomOnProduct();

        // when
        completionService.complete(productId, SELLER_ID, BUYER_ID);

        // then
        assertThat(notificationRepository.findAll()).singleElement().satisfies(n -> {
            assertThat(n.getRecipientId()).isEqualTo(BUYER_ID);
            assertThat(n.getType()).isEqualTo(NotificationType.SOLD);
            assertThat(n.getReferenceId()).isEqualTo(productId);
        });
    }

    private Long openRoomOnProduct() {
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        return productId;
    }
}
