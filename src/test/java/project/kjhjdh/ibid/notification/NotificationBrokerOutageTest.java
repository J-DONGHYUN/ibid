package project.kjhjdh.ibid.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.RabbitMQContainer;

import project.kjhjdh.ibid.chat.application.ChatMessageService;
import project.kjhjdh.ibid.chat.application.SendMessageCommand;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatMessageRepository;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;
import project.kjhjdh.ibid.trade.application.CompletionService;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationBrokerOutageTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Duration ACCEPTABLE_LATENCY = Duration.ofSeconds(5);

    @Autowired
    private RabbitMQContainer rabbitContainer;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private CompletionService completionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @DisplayName("[I-11] 브로커가 내려가 있어도 메시지 전송은 성공하고 알림만 유실된다 (QA-A.2)")
    @Test
    void sendSucceedsWhileBrokerIsDown() {
        // given — 구매자가 보낸 메시지의 수신자(판매자)는 방을 보고 있지 않다. 브로커가 내려간다
        Long productId = saveProduct();
        Long roomId = chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();
        rabbitContainer.stop();

        // when — 장애 중에 메시지를 보낸다
        long startedAt = System.nanoTime();
        boolean created = chatMessageService.send(new SendMessageCommand(roomId, BUYER_ID, "c-1", "안녕하세요")).created();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        // then — 메시지는 저장되고, 응답이 장애에 끌려 늦어지지 않으며, 알림은 유실된다
        assertThat(created).isTrue();
        assertThat(chatMessageRepository.findByChatRoomIdAndClientMessageId(roomId, "c-1")).isPresent();
        assertThat(elapsed).as("브로커 다운 중 전송 응답 시간").isLessThan(ACCEPTABLE_LATENCY);
        assertThat(notificationRepository.findAll()).as("유실된 알림 수").isEmpty();
    }

    @DisplayName("[I-11] 브로커가 내려가 있어도 거래완료는 성공하고 알림만 유실된다 (QA-A.2 · A.3)")
    @Test
    void completeSucceedsWhileBrokerIsDown() {
        // given
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        rabbitContainer.stop();

        // when — 장애 중에 거래완료한다 (커밋 뒤 발행이 실패한다)
        long startedAt = System.nanoTime();
        completionService.complete(productId, SELLER_ID, BUYER_ID);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        // then — 거래는 성립하고, 응답이 늦어지지 않으며, 알림은 유실된다 (ADR-0009 가 받아들인 대가)
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(product.getSoldBuyerId()).isEqualTo(BUYER_ID);
        assertThat(elapsed).as("브로커 다운 중 거래완료 응답 시간").isLessThan(ACCEPTABLE_LATENCY);
        assertThat(notificationRepository.findAll()).as("유실된 알림 수").isEmpty();
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
