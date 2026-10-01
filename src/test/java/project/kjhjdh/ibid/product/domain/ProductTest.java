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
import project.kjhjdh.ibid.product.DeviceSpecFixture;

class ProductTest {

    private static final Long SELLER_ID = 1L;

    @DisplayName("[PD-01] 유효한 값으로 상품을 생성하면 바로 판매중 상태가 된다")
    @Test
    void create() {
        // when
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());

        // then
        assertThat(product.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(product.getTitle()).isEqualTo("나이키 후드");
        assertThat(product.getPrice()).isEqualTo(89000);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getViewCount()).isZero();
    }

    @DisplayName("제목이 비었거나 100자를 초과하면 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000A"})
    void create_invalidTitle(String title) {
        // when & then
        assertThatThrownBy(() -> Product.create(SELLER_ID, title, "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PRODUCT_TITLE.getMessage());
    }

    @DisplayName("설명이 비어 있으면 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void create_invalidDescription(String description) {
        // when & then
        assertThatThrownBy(() -> Product.create(SELLER_ID, "나이키 후드", description, 89000, ProductCondition.USED, DeviceSpecFixture.sample()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PRODUCT_DESCRIPTION.getMessage());
    }

    @DisplayName("판매가가 1원 미만이면 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void create_invalidPrice(int price) {
        // when & then
        assertThatThrownBy(() -> Product.create(SELLER_ID, "나이키 후드", "상태 좋음", price, ProductCondition.USED, DeviceSpecFixture.sample()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PRODUCT_PRICE.getMessage());
    }

    @DisplayName("[I-01] 판매중 상품을 예약하면 예약중이 되고 예약 상대가 지정된다")
    @Test
    void reserve() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());

        // when
        product.reserve(2L);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.RESERVED);
        assertThat(product.getReservedBuyerId()).isEqualTo(2L);
    }

    @DisplayName("[TR-01] 판매중이 아닌 상품은 예약할 수 없다")
    @Test
    void reserve_notOnSale() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.reserve(2L);

        // when & then
        assertThatThrownBy(() -> product.reserve(3L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_ON_SALE.getMessage());
    }

    @DisplayName("[TR-02] 예약중 상품을 예약 해제하면 판매중이 되고 예약 상대가 비워진다")
    @Test
    void cancelReservation() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.reserve(2L);

        // when
        product.cancelReservation();

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getReservedBuyerId()).isNull();
    }

    @DisplayName("[TR-02] 예약중이 아닌 상품은 예약 해제할 수 없다")
    @Test
    void cancelReservation_notReserved() {
        // given — 판매중
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());

        // when & then
        assertThatThrownBy(product::cancelReservation)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_RESERVED.getMessage());
    }

    @DisplayName("[TR-03] 판매중 상품을 거래완료하면 거래완료 상태가 되고 거래 상대가 지정된다")
    @Test
    void complete_fromOnSale() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());

        // when
        product.complete(2L);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(product.getSoldBuyerId()).isEqualTo(2L);
    }

    @DisplayName("[TR-03][I-02] 예약중 상품을 거래완료하면 예약 상대가 비워지고 거래 상대가 정확히 한 명 지정된다")
    @Test
    void complete_fromReserved() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.reserve(2L);

        // when
        product.complete(3L);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(product.getSoldBuyerId()).isEqualTo(3L);
        assertThat(product.getReservedBuyerId()).isNull();
    }

    @DisplayName("[I-03] 거래완료 상품은 다시 거래완료할 수 없다")
    @Test
    void complete_alreadySold() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.complete(2L);

        // when & then
        assertThatThrownBy(() -> product.complete(3L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_ALREADY_SOLD.getMessage());
    }

    @DisplayName("[I-03] 거래완료 상품은 예약할 수 없다")
    @Test
    void reserve_whenSold() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.complete(2L);

        // when & then
        assertThatThrownBy(() -> product.reserve(3L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_ON_SALE.getMessage());
    }

    @DisplayName("[I-03] 거래완료 상품은 예약 해제할 수 없다")
    @Test
    void cancelReservation_whenSold() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.complete(2L);

        // when & then
        assertThatThrownBy(product::cancelReservation)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_RESERVED.getMessage());
    }

    @DisplayName("판매자 본인 여부를 판별한다")
    @Test
    void isOwnedBy() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());

        // when & then
        assertThat(product.isOwnedBy(SELLER_ID)).isTrue();
        assertThat(product.isOwnedBy(2L)).isFalse();
    }

    @DisplayName("이미지를 추가하면 기존 뒤에 순서대로 쌓인다")
    @Test
    void addImages() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());

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
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.addImages(List.of("u1.jpg", "u2.png", "u3.gif"));

        // when
        List<String> removed = product.removeImages(List.of("u1.jpg", "u3.gif"));

        // then
        assertThat(removed).containsExactly("u1.jpg", "u3.gif");
        assertThat(product.imageUrls()).containsExactly("u2.png");
    }
}
