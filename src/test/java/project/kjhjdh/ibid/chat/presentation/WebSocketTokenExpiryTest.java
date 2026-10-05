package project.kjhjdh.ibid.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import project.kjhjdh.ibid.auth.application.TokenProvider;
import project.kjhjdh.ibid.chat.application.ChatPresenceRegistry;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.StompTestClient;
import project.kjhjdh.ibid.support.WebIntegrationTestSupport;
import project.kjhjdh.ibid.user.domain.Role;

@TestPropertySource(properties = "jwt.access.expiration=2000")
class WebSocketTokenExpiryTest extends WebIntegrationTestSupport {

    private static final Long SELLER_ID = 980L;
    private static final Long BUYER_ID = 990L;
    private static final Duration PAST_EXPIRY = Duration.ofSeconds(3);

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

    @DisplayName("[CH-03] 연결할 때 낸 토큰이 만료돼도 열린 연결은 끊기지 않고, 만료 뒤 새 방 구독도 받아들여진다 (QA-D.2 — 현재 동작 고정)")
    @Test
    void connection_survivesTokenExpiry() throws Exception {
        // given — 수명 2초 토큰으로 연결해 한 방을 구독한다
        Long firstRoom = saveRoom();
        Long secondRoom = saveRoom();
        String token = tokenOf(BUYER_ID);
        try (StompTestClient buyer = StompTestClient.connect(port, token)) {
            buyer.subscribe("/topic/room." + firstRoom);
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(presenceRegistry.isViewing(firstRoom, BUYER_ID)).as("만료 전 구독 (ERROR: %s)", buyer.errors()).isTrue());

            // when — 토큰이 만료될 때까지 기다린 뒤 다른 방을 구독한다
            Thread.sleep(PAST_EXPIRY.toMillis());
            assertThatThrownBy(() -> tokenProvider.parseAccessToken(token)).as("전제: 같은 토큰은 이제 만료돼 파싱이 실패한다").isNotNull();
            buyer.subscribe("/topic/room." + secondRoom);

            // then — 연결이 끊기지 않고, 만료 뒤 구독도 받아들여진다 (연결 때만 토큰을 검증한다)
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(presenceRegistry.isViewing(secondRoom, BUYER_ID))
                            .as("만료 뒤 새 방 구독 여부 (연결 유지: %s, ERROR: %s)", buyer.isConnected(), buyer.errors()).isTrue());
            assertThat(buyer.isConnected()).as("만료 뒤 연결 유지 여부").isTrue();
            assertThat(buyer.errors()).as("만료 뒤 서버가 보낸 ERROR").isEmpty();
        }
    }

    @DisplayName("[CH-03] 만료된 토큰으로 연결된 구독도 만료 뒤에 방송된 메시지를 받는다 (QA-D.2 — 현재 동작 고정)")
    @Test
    void expiredSession_stillReceivesBroadcast() throws Exception {
        // given — 수명 2초 토큰으로 연결해 구독한 구매자. 토큰이 만료될 때까지 기다린다
        Long roomId = saveRoom();
        String token = tokenOf(BUYER_ID);
        try (StompTestClient buyer = StompTestClient.connect(port, token)) {
            buyer.subscribe("/topic/room." + roomId);
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(presenceRegistry.isViewing(roomId, BUYER_ID)).as("만료 전 구독 (ERROR: %s)", buyer.errors()).isTrue());
            Thread.sleep(PAST_EXPIRY.toMillis());
            assertThatThrownBy(() -> tokenProvider.parseAccessToken(token)).as("전제: 같은 토큰은 이제 만료돼 파싱이 실패한다").isNotNull();

            // when — 판매자가 (새로 발급한 유효한 토큰으로) REST 로 메시지를 보낸다
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tokenOf(SELLER_ID));
            ResponseEntity<String> sent = restTemplate.exchange("/api/chat-rooms/" + roomId + "/messages", HttpMethod.POST,
                    new HttpEntity<>(Map.of("clientMessageId", "c-1", "content", "만료 뒤 메시지"), headers), String.class);

            // then — 만료된 토큰의 세션이 방송을 받는다
            assertThat(sent.getStatusCode().value()).as("판매자 전송 응답").isEqualTo(201);
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(buyer.received()).as("만료된 세션이 받은 방송 수 (연결 유지: %s)", buyer.isConnected()).hasSize(1));
        }
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
