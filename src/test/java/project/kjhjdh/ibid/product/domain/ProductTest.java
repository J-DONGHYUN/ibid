package project.kjhjdh.ibid.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

class ProductTest {

    private static final Long SELLER_ID = 1L;
    private static final Long BUYER_ID = 2L;

    @DisplayName("유효한 값으로 상품을 생성하면 판매 대기 상태가 된다")
    @Test
    void create() {
        // when
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);

        // then
        assertThat(product.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(product.getTitle()).isEqualTo("나이키 후드");
        assertThat(product.getPrice()).isEqualTo(89000);
        assertThat(product.getStock()).isEqualTo(3);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.PENDING);
    }

    @DisplayName("제목이 비었거나 100자를 초과하면 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000A"})
    void create_invalidTitle(String title) {
        // when & then
        assertThatThrownBy(() -> Product.create(SELLER_ID, title, "상태 좋음", 89000, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PRODUCT_TITLE.getMessage());
    }

    @DisplayName("설명이 비어 있으면 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void create_invalidDescription(String description) {
        // when & then
        assertThatThrownBy(() -> Product.create(SELLER_ID, "나이키 후드", description, 89000, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PRODUCT_DESCRIPTION.getMessage());
    }

    @DisplayName("판매가가 1원 미만이면 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void create_invalidPrice(int price) {
        // when & then
        assertThatThrownBy(() -> Product.create(SELLER_ID, "나이키 후드", "상태 좋음", price, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PRODUCT_PRICE.getMessage());
    }

    @DisplayName("재고가 1개 미만이면 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void create_invalidStock(int stock) {
        // when & then
        assertThatThrownBy(() -> Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, stock))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PRODUCT_STOCK.getMessage());
    }

    @DisplayName("판매를 시작하면 판매중 상태가 된다")
    @Test
    void openForSale() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);

        // when
        product.openForSale();

        // then
        assertThat(product.isOnSale()).isTrue();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @DisplayName("판매 대기 상태가 아니면 판매를 시작할 수 없다")
    @Test
    void openForSale_notPending() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();

        // when & then
        assertThatThrownBy(product::openForSale)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_PENDING.getMessage());
    }

    @DisplayName("판매를 시작하지 않은 상품은 구매(차감)할 수 없다")
    @Test
    void decreaseStock_notOnSale() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_ON_SALE.getMessage());
    }

    @DisplayName("구매 수량만큼 재고를 차감한다")
    @Test
    void decreaseStock() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();

        // when
        product.decreaseStock(2);

        // then
        assertThat(product.getStock()).isEqualTo(1);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @DisplayName("재고가 0이 되면 품절 상태로 전환된다")
    @Test
    void decreaseStock_soldOut() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        product.openForSale();

        // when
        product.decreaseStock(1);

        // then
        assertThat(product.getStock()).isZero();
        assertThat(product.isSoldOut()).isTrue();
    }

    @DisplayName("구매 수량이 1개 미만이면 차감에 실패한다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void decreaseStock_invalidQuantity(int quantity) {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(quantity))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PURCHASE_QUANTITY.getMessage());
    }

    @DisplayName("구매 수량이 재고보다 많으면 차감에 실패한다")
    @Test
    void decreaseStock_insufficientStock() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        product.openForSale();

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(2))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());
    }

    @DisplayName("이미 품절된 상품은 차감에 실패한다")
    @Test
    void decreaseStock_alreadySoldOut() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        product.openForSale();
        product.decreaseStock(1);

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SOLD_OUT.getMessage());
    }

    @DisplayName("판매중이고 재고가 충분하면 구매 가능 검증을 통과하고 재고는 변하지 않는다")
    @Test
    void validatePurchasable() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();

        // when
        product.validatePurchasable(BUYER_ID, 2);

        // then
        assertThat(product.getStock()).isEqualTo(3);
    }

    @DisplayName("본인이 등록한 상품은 구매 가능 검증에 실패한다")
    @Test
    void validatePurchasable_selfTrade() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();

        // when & then
        assertThatThrownBy(() -> product.validatePurchasable(SELLER_ID, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SELF_TRADE_NOT_ALLOWED.getMessage());
    }

    @DisplayName("판매를 시작하지 않은 상품은 구매 가능 검증에 실패한다")
    @Test
    void validatePurchasable_notOnSale() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);

        // when & then
        assertThatThrownBy(() -> product.validatePurchasable(BUYER_ID, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_ON_SALE.getMessage());
    }

    @DisplayName("구매 수량이 재고보다 많으면 구매 가능 검증에 실패한다")
    @Test
    void validatePurchasable_insufficientStock() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        product.openForSale();

        // when & then
        assertThatThrownBy(() -> product.validatePurchasable(BUYER_ID, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());
        assertThat(product.getStock()).isEqualTo(1);
    }

    @DisplayName("이미 품절된 상품은 구매 가능 검증에 실패한다")
    @Test
    void validatePurchasable_soldOut() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        product.openForSale();
        product.decreaseStock(1);

        // when & then
        assertThatThrownBy(() -> product.validatePurchasable(BUYER_ID, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SOLD_OUT.getMessage());
    }

    @DisplayName("구매 수량이 1개 미만이면 구매 가능 검증에 실패한다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void validatePurchasable_invalidQuantity(int quantity) {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();

        // when & then
        assertThatThrownBy(() -> product.validatePurchasable(BUYER_ID, quantity))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PURCHASE_QUANTITY.getMessage());
    }

    @DisplayName("취소된 수량만큼 재고를 복원한다")
    @Test
    void restoreStock() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();
        product.decreaseStock(2);

        // when
        product.restoreStock(2);

        // then
        assertThat(product.getStock()).isEqualTo(3);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @DisplayName("품절된 상품에 재고가 복원되면 다시 판매중이 된다")
    @Test
    void restoreStock_reopensSoldOut() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        product.openForSale();
        product.decreaseStock(1);

        // when
        product.restoreStock(1);

        // then
        assertThat(product.getStock()).isEqualTo(1);
        assertThat(product.isOnSale()).isTrue();
    }

    @DisplayName("복원 수량이 1개 미만이면 실패한다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void restoreStock_invalidQuantity(int quantity) {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();

        // when & then
        assertThatThrownBy(() -> product.restoreStock(quantity))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PURCHASE_QUANTITY.getMessage());
    }

    @DisplayName("판매 시작 전(PENDING) 상품은 재고를 복원할 수 없다")
    @Test
    void restoreStock_pending() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);

        // when & then
        assertThatThrownBy(() -> product.restoreStock(1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CANNOT_RESTORE_STOCK.getMessage());
    }

    @DisplayName("판매자 본인 여부를 판별한다")
    @Test
    void isOwnedBy() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);

        // when & then
        assertThat(product.isOwnedBy(SELLER_ID)).isTrue();
        assertThat(product.isOwnedBy(2L)).isFalse();
    }

    @DisplayName("배송비가 0원 미만이면 생성에 실패한다")
    @Test
    void create_invalidShippingFee() {
        // when & then
        assertThatThrownBy(() -> Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3,
                ProductCondition.USED, List.of(Tag.of("태그")), -1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_SHIPPING_FEE.getMessage());
    }

    @DisplayName("이미지를 추가하면 기존 뒤에 순서대로 쌓인다")
    @Test
    void addImages() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);

        // when
        product.addImages(List.of("u1.jpg", "u2.png"));
        product.addImages(List.of("u3.gif"));

        // then
        assertThat(product.imageUrls()).containsExactly("u1.jpg", "u2.png", "u3.gif");
    }

    @DisplayName("선택한 이미지를 제거하고 제거된 URL을 돌려준다")
    @Test
    void removeImages() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.addImages(List.of("u1.jpg", "u2.png", "u3.gif"));

        // when
        List<String> removed = product.removeImages(List.of("u1.jpg", "u3.gif"));

        // then
        assertThat(removed).containsExactly("u1.jpg", "u3.gif");
        assertThat(product.imageUrls()).containsExactly("u2.png");
    }
}
