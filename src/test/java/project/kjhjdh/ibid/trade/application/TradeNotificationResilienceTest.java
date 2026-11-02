package project.kjhjdh.ibid.trade.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.notification.application.NotificationService;
import project.kjhjdh.ibid.notification.domain.NotificationType;
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
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @MockitoBean
    private NotificationService notificationService;

    @DisplayName("[I-11] 알림 저장이 실패해도 거래완료는 성공한다")
    @Test
    void completeSucceedsWhenNotificationSaveFails() {
        // given — 알림 저장이 던지도록 (ADR-0014 — 리스너가 흡수해야 한다)
        willThrow(new DataIntegrityViolationException("notification insert failed"))
                .given(notificationService).save(anyLong(), any(NotificationType.class), anyLong());
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));

        // when — 거래완료
        completionService.complete(productId, SELLER_ID, BUYER_ID);

        // then — 알림 저장 실패와 무관하게 상품은 거래완료 상태로 커밋된다
        //        (커밋 후 전달이라 전이는 이미 끝났고, 리스너가 예외를 흡수한다)
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(product.getSoldBuyerId()).isEqualTo(BUYER_ID);
    }
}
