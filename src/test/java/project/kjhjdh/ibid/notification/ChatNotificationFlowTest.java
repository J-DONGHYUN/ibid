package project.kjhjdh.ibid.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.chat.application.ChatMessageService;
import project.kjhjdh.ibid.chat.application.ChatPresenceRegistry;
import project.kjhjdh.ibid.chat.application.SendMessageCommand;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.notification.domain.NotificationType;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

/**
 * 채팅 전송에서 알림 행까지를 끊지 않고 잇는다.
 * 전송은 트랜잭션이 없어 리스너가 fallbackExecution 으로 돌아야 한다 (ADR-0014).
 */
class ChatNotificationFlowTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final String VIEWING_SESSION = "session-1";

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ChatPresenceRegistry presenceRegistry;

    // ChatPresenceRegistry 는 싱글턴이라 DbCleaner 가 치우지 못한다.
    // TRUNCATE 로 AUTO_INCREMENT 가 리셋돼 다음 테스트가 같은 방 id 를 받으므로,
    // 치우지 않으면 메서드 실행 순서에 따라 결과가 뒤집힌다.
    @AfterEach
    void clearPresence() {
        presenceRegistry.disconnect(VIEWING_SESSION);
    }

    @DisplayName("[NT-01] 메시지를 보내면 그 방을 안 보는 상대에게 알림 행이 생긴다 (트랜잭션 없는 경로)")
    @Test
    void sendStoresNotificationForAbsentRecipient() {
        // given — seller 는 그 방을 보고 있지 않다
        Long roomId = openRoom();

        // when
        chatMessageService.send(new SendMessageCommand(roomId, BUYER_ID, "c-1", "7시에 봬요"));

        // then — 발행만이 아니라 저장까지 확인한다
        assertThat(notificationRepository.findAll()).singleElement().satisfies(n -> {
            assertThat(n.getRecipientId()).isEqualTo(SELLER_ID);
            assertThat(n.getType()).isEqualTo(NotificationType.NEW_MESSAGE);
            assertThat(n.getReferenceId()).isEqualTo(roomId);
        });
    }

    @DisplayName("[NT-01] 상대가 그 방을 보고 있으면 알림 행이 생기지 않는다")
    @Test
    void sendSkipsNotificationWhenRecipientViewing() {
        // given — seller 가 그 방을 보는 중
        Long roomId = openRoom();
        presenceRegistry.subscribe(VIEWING_SESSION, "sub-0", SELLER_ID, roomId);

        // when
        chatMessageService.send(new SendMessageCommand(roomId, BUYER_ID, "c-1", "7시에 봬요"));

        // then
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    private Long openRoom() {
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        return chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();
    }
}
