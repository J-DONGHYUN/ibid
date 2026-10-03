package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.application.ProductService;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;
import project.kjhjdh.ibid.trade.application.CompletionService;

class ChatRoomOpenRestrictionTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Long OTHER_BUYER_ID = 30L;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private CompletionService completionService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @DisplayName("[CH-01] 삭제된 상품에는 새 채팅방을 열 수 없다 (QA-1.3)")
    @Test
    @Disabled("QA-1.3 발견 — 삭제 상품에 채팅방이 열린다. T-42 에서 막고 이 줄을 지운다")
    void open_deletedProduct() {
        // given — 판매자가 상품을 삭제한다 (채팅방은 아직 없다)
        Long productId = saveProduct();
        productService.delete(SELLER_ID, productId);

        // when
        assertThatThrownBy(() -> chatRoomService.open(productId, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());

        // then — 방이 생기지 않는다
        assertThat(chatRoomRepository.findByProductIdAndBuyerId(productId, BUYER_ID)).isEmpty();
    }

    @DisplayName("[CH-01] 거래완료된 상품에는 새 채팅방을 열 수 없다 (QA-1.4)")
    @Test
    @Disabled("QA-1.4 발견 — 거래완료 상품에 채팅방이 열린다. T-42 에서 막고 이 줄을 지운다")
    void open_soldProduct() {
        // given — 기존 구매자와 거래완료된 상품
        Long productId = saveProduct();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        completionService.complete(productId, SELLER_ID, BUYER_ID);

        // when — 거래와 무관한 다른 구매자가 채팅방을 연다
        assertThatThrownBy(() -> chatRoomService.open(productId, OTHER_BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_ALREADY_SOLD.getMessage());

        // then
        assertThat(chatRoomRepository.findByProductIdAndBuyerId(productId, OTHER_BUYER_ID)).isEmpty();
    }

    @DisplayName("[CH-01] 내 상품에는 채팅방을 열 수 없다 (QA-1.7)")
    @Test
    void open_ownProduct() {
        // given
        Long productId = saveProduct();

        // when — 판매자 본인이 자기 상품에 채팅방을 연다
        assertThatThrownBy(() -> chatRoomService.open(productId, SELLER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CANNOT_OPEN_CHAT_ON_OWN_PRODUCT.getMessage());

        // then
        assertThat(chatRoomRepository.findByProductIdAndBuyerId(productId, SELLER_ID)).isEmpty();
    }

    @DisplayName("[CH-01] 거래완료 전에 열린 채팅방은 거래완료 뒤에도 그대로 돌아온다 (QA-1.4 대조군)")
    @Test
    void open_existingRoomAfterSold() {
        // given — 거래 상대의 방은 거래완료 전에 이미 열려 있다
        Long productId = saveProduct();
        Long roomId = chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();
        completionService.complete(productId, SELLER_ID, BUYER_ID);

        // when — 같은 구매자가 다시 연다 (멱등)
        ChatRoom room = chatRoomService.open(productId, BUYER_ID);

        // then — 새 방이 아니라 기존 방이다. 인수 조율 대화가 끊기지 않는다
        assertThat(room.getId()).isEqualTo(roomId);
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
