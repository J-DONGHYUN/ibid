package project.kjhjdh.ibid.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import project.kjhjdh.ibid.auth.application.TokenProvider;
import project.kjhjdh.ibid.chat.application.ChatPresenceRegistry;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.WebIntegrationTestSupport;
import project.kjhjdh.ibid.user.domain.Role;

class WebSocketSubscribeTest extends WebIntegrationTestSupport {

    private static final Long SELLER_ID = 930L;
    private static final Long BUYER_ID = 940L;

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

    @DisplayName("[CH-03] 유효한 토큰으로 연결한 참여자가 자기 채팅방을 구독하면 구독이 받아들여진다 (QA-D.2a)")
    @Test
    @Disabled("QA-D.2a 발견 — 유효한 토큰으로 연결해도 SUBSCRIBE 가 UNAUTHORIZED 로 거부되고 연결이 끊긴다. T-49 에서 고치고 이 줄을 지운다")
    void participantCanSubscribeOwnRoom() throws Exception {
        // given — 참여자(구매자)의 유효한 토큰으로 CONNECT 한다
        Long roomId = saveRoom();
        String token = tokenProvider.createTokenPair(BUYER_ID, Role.USER).accessToken();
        CopyOnWriteArrayList<String> errors = new CopyOnWriteArrayList<>();
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        errors.add(String.valueOf(headers.getFirst("message")));
                    }
                }).get(10, TimeUnit.SECONDS);

        try {
            // when — 같은 연결로 자기 방을 구독한다
            session.subscribe("/topic/room." + roomId, new StompSessionHandlerAdapter() { });

            // then — 구독이 받아들여져 "보는 중" 이 되고 연결이 유지된다. 서버가 ERROR 를 돌려주면 안 된다
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(presenceRegistry.isViewing(roomId, BUYER_ID))
                            .as("구독 뒤 보는 중 여부 (서버가 돌려준 ERROR: %s)", errors).isTrue());
            assertThat(session.isConnected()).isTrue();
        } finally {
            if (session.isConnected()) {
                session.disconnect();
            }
            client.stop();
        }
    }

    private Long saveRoom() {
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        return chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();
    }
}
