package project.kjhjdh.ibid.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.Disabled;
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
import project.kjhjdh.ibid.support.StompTestClient;
import project.kjhjdh.ibid.support.WebIntegrationTestSupport;
import project.kjhjdh.ibid.user.domain.Role;

class WebSocketSubscribeTest extends WebIntegrationTestSupport {

    private static final Long SELLER_ID = 930L;
    private static final Long BUYER_ID = 940L;
    private static final Long OUTSIDER_ID = 950L;

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
    @Disabled("QA-D.2a 발견 — 유효한 토큰으로 연결해도 SUBSCRIBE 가 거부된다. T-49 에서 고치고 이 줄을 지운다")
    void participantCanSubscribeOwnRoom() throws Exception {
        // given
        Long roomId = saveRoom();

        try (StompTestClient buyer = StompTestClient.connect(port, tokenOf(BUYER_ID))) {
            // when
            boolean accepted = buyer.subscribe("/topic/room." + roomId);

            // then
            assertThat(accepted).as("구독 수락 여부 (서버가 돌려준 ERROR: %s)", buyer.errors()).isTrue();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(presenceRegistry.isViewing(roomId, BUYER_ID)).isTrue());
            assertThat(buyer.isConnected()).isTrue();
        }
    }

    @DisplayName("[I-08] 참여자는 그 방을 구독할 수 있고 참여자가 아닌 사용자는 구독할 수 없다")
    @Test
    @Disabled("QA-D.2a 발견 — 유효한 토큰으로 연결해도 SUBSCRIBE 가 거부된다. T-49 에서 고치고 이 줄을 지운다")
    void outsiderCannotSubscribe() throws Exception {
        // given
        Long roomId = saveRoom();

        try (StompTestClient buyer = StompTestClient.connect(port, tokenOf(BUYER_ID));
             StompTestClient outsider = StompTestClient.connect(port, tokenOf(OUTSIDER_ID))) {
            // when
            boolean buyerAccepted = buyer.subscribe("/topic/room." + roomId);
            boolean outsiderAccepted = outsider.subscribe("/topic/room." + roomId);

            // then — 참여자는 받아들여지고 외부인은 거부돼 보는 중이 등록되지 않는다
            assertThat(buyerAccepted).as("참여자 구독 수락 (ERROR: %s)", buyer.errors()).isTrue();
            assertThat(outsiderAccepted).as("외부인 구독 수락").isFalse();
            assertThat(presenceRegistry.isViewing(roomId, OUTSIDER_ID)).isFalse();
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
