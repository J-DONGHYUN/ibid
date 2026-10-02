package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.chat.application.ChatMessageService;
import project.kjhjdh.ibid.chat.application.ChatRoomService;
import project.kjhjdh.ibid.chat.application.SendMessageCommand;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.DeviceCategory;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.trade.application.TradeHistoryService;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductDeletionIntegrationTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final String VISITOR_ID = "visitor-a";

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductLikeService productLikeService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private TradeHistoryService tradeHistoryService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @DisplayName("[PD-06] 상품을 삭제하면 목록·상세·관심목록에서 숨고, 채팅방은 남지만 메시지는 못 보낸다")
    @Test
    void softDelete_surface() {
        // given — 상품 등록 + 채팅방 + 찜
        Long productId = productService.register(SELLER_ID, registerCommand());
        Long roomId = chatRoomService.open(productId, BUYER_ID).getId();
        productLikeService.like(BUYER_ID, productId);

        // when — 삭제
        productService.delete(SELLER_ID, productId);

        // then — 목록에서 숨는다
        assertThat(ids(productService.getProducts(null, true))).doesNotContain(productId);
        // 상세는 404
        assertThatThrownBy(() -> productService.getProduct(productId, VISITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        // 관심목록에서 숨는다
        assertThat(productLikeService.myLikedProducts(BUYER_ID)).isEmpty();
        // 채팅방은 남는다
        assertThat(chatRoomRepository.findById(roomId)).isPresent();
        // 그 방엔 새 메시지를 못 보낸다 (I-09)
        assertThatThrownBy(() -> chatMessageService.send(new SendMessageCommand(roomId, BUYER_ID, "c-1", "안녕하세요")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_DELETED.getMessage());
        // 판매 내역엔 남는다 (보여줄 경로)
        assertThat(ids(tradeHistoryService.getSales(SELLER_ID, null, null))).contains(productId);
    }

    private java.util.List<Long> ids(ProductListResult result) {
        return result.slice().getContent().stream().map(Product::getId).toList();
    }

    private ProductRegisterCommand registerCommand() {
        return new ProductRegisterCommand("아이폰 13", "A급", 500000, project.kjhjdh.ibid.product.domain.ProductCondition.USED,
                new DeviceSpecCommand(DeviceCategory.SMARTPHONE, "iPhone 13", 90, "본체", null));
    }
}
