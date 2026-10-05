package project.kjhjdh.ibid.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import project.kjhjdh.ibid.auth.application.TokenProvider;
import project.kjhjdh.ibid.chat.application.ChatPresenceRegistry;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.StompFrameSocket;
import project.kjhjdh.ibid.support.WebIntegrationTestSupport;
import project.kjhjdh.ibid.user.domain.Role;

class WebSocketAbruptCloseTest extends WebIntegrationTestSupport {

    private static final Long SELLER_ID = 960L;
    private static final Long BUYER_ID = 970L;

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

    @DisplayName("[NT-01] 구독한 뒤 STOMP DISCONNECT 없이 WebSocket 연결만 닫아도 보는 중 등록이 정리된다 (QA-D.3)")
    @Test
    void presence_isClearedWhenConnectionClosesWithoutDisconnect() throws Exception {
        // given — 구매자가 자기 방을 구독해 보는 중으로 등록된다
        Long roomId = saveRoom();
        try (StompFrameSocket buyer = StompFrameSocket.connect(port, tokenOf(BUYER_ID))) {
            buyer.subscribe("sub-0", "/topic/room." + roomId);
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(presenceRegistry.isViewing(roomId, BUYER_ID))
                            .as("구독 뒤 보는 중 여부 (받은 프레임: %s)", buyer.frames()).isTrue());

            // when — DISCONNECT 프레임을 보내지 않고 WebSocket 연결만 닫는다
            buyer.closeWithoutDisconnect();

            // then — 세션 종료가 감지돼 보는 중 등록이 정리된다 (안 그러면 알림이 영구 억제된다)
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(presenceRegistry.isViewing(roomId, BUYER_ID))
                            .as("DISCONNECT 없이 닫은 뒤 보는 중 여부").isFalse());
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
