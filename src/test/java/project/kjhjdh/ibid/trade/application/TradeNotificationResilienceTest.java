package project.kjhjdh.ibid.trade.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import project.kjhjdh.ibid.chat.application.ChatMessageService;
import project.kjhjdh.ibid.chat.application.SendMessageCommand;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatMessageRepository;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.notification.domain.Notification;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class TradeNotificationResilienceTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;

    @Autowired
    private CompletionService completionService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // 실제 서비스 · 실제 트랜잭션 경계를 지나게 두고, 저장만 던지게 한다.
    // 서비스를 mock 하면 트랜잭션 프록시가 없어 참여 실패가 재현되지 않는다.
    @MockitoBean
    private NotificationRepository notificationRepository;

    @DisplayName("[I-11] 알림 저장이 실패해도 거래완료는 성공한다")
    @Test
    void completeSucceedsWhenNotificationSaveFails() {
        // given — 알림 저장이 던진다 (트랜잭션 경계 안에서)
        willThrow(new DataIntegrityViolationException("notification insert failed"))
                .given(notificationRepository).save(any(Notification.class));
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));

        // when · then — 호출자에게 예외가 새지 않는다
        assertThatCode(() -> completionService.complete(productId, SELLER_ID, BUYER_ID))
                .doesNotThrowAnyException();

        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(product.getSoldBuyerId()).isEqualTo(BUYER_ID);
    }

    @DisplayName("[I-11] 알림 저장이 실패해도 메시지 전송은 성공한다")
    @Test
    void sendSucceedsWhenNotificationSaveFails() {
        // given — 트랜잭션이 없는 경로. 리스너가 호출자 스레드에서 인라인으로 돈다
        willThrow(new DataIntegrityViolationException("notification insert failed"))
                .given(notificationRepository).save(any(Notification.class));
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        Long roomId = chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();

        // when · then — 호출자에게 예외가 새지 않는다
        assertThatCode(() -> chatMessageService.send(
                new SendMessageCommand(roomId, BUYER_ID, "c-1", "7시에 봬요")))
                .doesNotThrowAnyException();

        assertThat(chatMessageRepository.count()).as("메시지는 저장돼 있다").isEqualTo(1);
    }
}
