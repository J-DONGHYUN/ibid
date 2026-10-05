package project.kjhjdh.ibid.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.times;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import project.kjhjdh.ibid.auth.application.TokenProvider;
import project.kjhjdh.ibid.chat.application.ChatPresenceRegistry;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.event.NotificationEventPublisher;
import project.kjhjdh.ibid.common.event.NotificationMessage;
import project.kjhjdh.ibid.common.event.NotificationMessage.NotificationType;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.StompTestClient;
import project.kjhjdh.ibid.support.WebIntegrationTestSupport;
import project.kjhjdh.ibid.user.domain.Role;

class WebSocketRealtimeTest extends WebIntegrationTestSupport {

    private static final Long SELLER_ID = 960L;
    private static final Long BUYER_ID = 970L;
    private static final String READY_CONTENT = "준비 확인";
    private static final long NOTIFICATION_WAIT_MILLIS = 300;

    @LocalServerPort
    private int port;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ChatPresenceRegistry presenceRegistry;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @MockitoBean
    private NotificationEventPublisher notificationEventPublisher;

    @DisplayName("[CH-03] 구독한 연결은 판매자가 보낸 메시지를 새로고침 없이 보낸 순서대로 받는다")
    @Test
    void subscriberReceivesMessagesInOrder() throws Exception {
        // given
        Long roomId = saveRoom();

        try (StompTestClient buyer = StompTestClient.connect(port, tokenOf(BUYER_ID))) {
            awaitSubscribed(buyer, roomId);

            // when — 판매자가 REST 로 연속 세 건을 보낸다
            send(roomId, "m-1", "첫 번째");
            send(roomId, "m-2", "두 번째");
            send(roomId, "m-3", "세 번째");

            // then — 구매자는 새로고침 없이 세 건을 같은 순서로 받는다 (준비 확인용 메시지는 걸러 본다)
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(contentsOf(buyer)).as("수신한 본 메시지 (ERROR: %s)", buyer.errors()).hasSize(3));
            assertThat(contentsOf(buyer)).containsExactly("첫 번째", "두 번째", "세 번째");
        }
    }

    @DisplayName("[NT-01] 방을 보고 있는 참여자에게는 알림이 가지 않고 보고 있지 않으면 간다")
    @Test
    void notificationIsSuppressedOnlyWhileViewing() throws Exception {
        // given
        Long roomId = saveRoom();
        NotificationMessage expected = new NotificationMessage(BUYER_ID, NotificationType.NEW_MESSAGE, roomId);

        // when — 구매자가 방을 보는 동안 판매자가 보낸다
        try (StompTestClient buyer = StompTestClient.connect(port, tokenOf(BUYER_ID))) {
            awaitSubscribed(buyer, roomId);
            send(roomId, "m-1", "보는 중에 보냄");

            // then — 알림이 발행되지 않는다
            then(notificationEventPublisher).should(after(NOTIFICATION_WAIT_MILLIS).never()).publish(expected);
        }

        // when — 구매자가 연결을 닫아 방을 떠난 뒤 판매자가 다시 보낸다
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(presenceRegistry.isViewing(roomId, BUYER_ID)).isFalse());
        send(roomId, "m-2", "떠난 뒤에 보냄");

        // then — 알림이 한 번 발행된다
        then(notificationEventPublisher).should(times(1)).publish(expected);
    }

    private void awaitSubscribed(StompTestClient buyer, Long roomId) {
        buyer.subscribe("/topic/room." + roomId);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(presenceRegistry.isViewing(roomId, BUYER_ID))
                        .as("구독 뒤 보는 중 여부 (서버가 돌려준 ERROR: %s)", buyer.errors()).isTrue());
        AtomicInteger sequence = new AtomicInteger();
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).until(() -> {
            send(roomId, "ready-" + sequence.incrementAndGet(), READY_CONTENT);
            return !buyer.received().isEmpty();
        });
    }

    private List<Object> contentsOf(StompTestClient buyer) {
        return buyer.received().stream()
                .map(message -> message.get("content"))
                .filter(content -> !READY_CONTENT.equals(content))
                .toList();
    }

    private void send(Long roomId, String clientMessageId, String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenOf(SELLER_ID));
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chat-rooms/" + roomId + "/messages", HttpMethod.POST,
                new HttpEntity<>(Map.of("content", content, "clientMessageId", clientMessageId), headers),
                String.class);
        assertThat(response.getStatusCode()).as("REST 전송 응답").isEqualTo(HttpStatus.CREATED);
    }

    private String tokenOf(Long userId) {
        return tokenProvider.createTokenPair(userId, Role.USER).accessToken();
    }

    private Long saveRoom() {
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        return chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();
    }
}
