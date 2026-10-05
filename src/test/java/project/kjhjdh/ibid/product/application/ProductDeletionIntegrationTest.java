package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.chat.application.ChatMessageService;
import project.kjhjdh.ibid.chat.application.ChatRoomService;
import project.kjhjdh.ibid.chat.application.MyChatRoomResult;
import project.kjhjdh.ibid.chat.application.SendMessageCommand;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.DeviceCategory;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.trade.application.CompletionService;
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
    private CompletionService completionService;

    @Autowired
    private TradeHistoryService tradeHistoryService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @DisplayName("[PD-06] 삭제하면 목록·상세·관심목록에서 숨고, 채팅방·내역엔 남으며 메시지는 못 보낸다")
    @Test
    void softDelete_surface() {
        // given — 상품 등록 + 채팅방 + 찜 + 메시지 + 거래완료(구매 내역에 남도록)
        Long productId = productService.register(SELLER_ID, registerCommand());
        Long roomId = chatRoomService.open(productId, BUYER_ID).getId();
        productLikeService.like(BUYER_ID, productId);
        chatMessageService.send(new SendMessageCommand(roomId, BUYER_ID, "c-1", "삭제 전 메시지"));
        completionService.complete(productId, SELLER_ID, BUYER_ID);

        // when — 삭제 (거래완료 상품도 삭제 가능)
        productService.delete(SELLER_ID, productId);

        // then — 숨길 경로
        assertThat(ids(productService.getProducts(null, true))).doesNotContain(productId);
        assertThatThrownBy(() -> productService.getProduct(productId, VISITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        assertThat(productLikeService.myLikedProducts(BUYER_ID)).isEmpty();

        // then — 보여줄 경로 (삭제분 보존)
        assertThat(ids(tradeHistoryService.getSales(SELLER_ID, null, null))).contains(productId);     // TR-04
        assertThat(ids(tradeHistoryService.getPurchases(BUYER_ID, null))).contains(productId);        // TR-05
        assertThat(myRoomProductIds()).contains(productId);                                           // CH-05 (채팅방 목록에 상품 요약 남음)

        // then — 채팅방은 남고, 새 메시지는 못 보낸다 (I-09)
        assertThat(chatRoomRepository.findById(roomId)).isPresent();
        assertThatThrownBy(() -> chatMessageService.send(new SendMessageCommand(roomId, BUYER_ID, "c-2", "삭제 후 메시지")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_DELETED.getMessage());
    }

    @DisplayName("[PD-06] 삭제된 상품의 수정 · 이미지 변경 · 재삭제는 404 다 (쓰기 경로 전용 조회, 동작 불변)")
    @Test
    void softDelete_writesAreNotFound() {
        // given
        Long productId = productService.register(SELLER_ID, registerCommand());
        productService.delete(SELLER_ID, productId);
        ProductUpdateCommand update = new ProductUpdateCommand("수정", "설명", 1000, ProductCondition.USED,
                new DeviceSpecCommand(DeviceCategory.SMARTPHONE, "iPhone 13", 90, "본체", null));

        // when & then
        assertNotFound(() -> productService.update(SELLER_ID, productId, update));
        assertNotFound(() -> productService.delete(SELLER_ID, productId));
        assertNotFound(() -> productService.confirmImages(SELLER_ID, productId, java.util.List.of("https://x/a.png")));
        assertNotFound(() -> productService.deleteImages(SELLER_ID, productId, java.util.List.of("https://x/a.png")));
        assertNotFound(() -> productService.generatePresignedUrls(SELLER_ID, productId,
                java.util.List.of(new ImagePresignCommand("front.png", "image/png"))));
    }

    private void assertNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    private java.util.List<Long> ids(ProductListResult result) {
        return result.slice().getContent().stream().map(Product::getId).toList();
    }

    private java.util.List<Long> myRoomProductIds() {
        return chatRoomService.getMyRooms(BUYER_ID, null).getContent().stream()
                .map(MyChatRoomResult::product)
                .filter(java.util.Objects::nonNull)
                .map(summary -> summary.productId())
                .toList();
    }

    private ProductRegisterCommand registerCommand() {
        return new ProductRegisterCommand("아이폰 13", "A급", 500000, ProductCondition.USED,
                new DeviceSpecCommand(DeviceCategory.SMARTPHONE, "iPhone 13", 90, "본체", null));
    }
}
