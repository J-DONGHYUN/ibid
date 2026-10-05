package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class DeletedProductLikeTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long USER_ID = 20L;

    @Autowired
    private ProductLikeService productLikeService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[PD-07] 삭제된 상품은 찜할 수 없다 (QA-1.5)")
    @Test
    @Disabled("QA-1.5 발견 — 삭제된 상품도 찜이 된다. T-53 에서 404 로 막고 이 줄을 지운다")
    void like_deletedProduct() {
        // given — 판매자가 삭제한 상품
        Long productId = saveProduct();
        productService.delete(SELLER_ID, productId);

        // when & then — 찜하기는 404 다
        assertThatThrownBy(() -> productLikeService.like(USER_ID, productId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        assertThat(productLikeService.isLiked(USER_ID, productId)).isFalse();
    }

    @DisplayName("[PD-07] 삭제된 상품에 여러 사용자가 찜해도 저장되는 찜이 없다 (QA-1.5 측정)")
    @Test
    @Disabled("QA-1.5 발견 — 삭제된 상품에 3명이 찜하면 읽히지 않는 찜 행이 3개 쌓인다(실측 3). T-53 에서 막고 이 줄을 지운다")
    void like_deletedProduct_storesNoLikeRow() {
        // given — 삭제된 상품과 서로 다른 사용자 3명
        Long productId = saveProduct();
        productService.delete(SELLER_ID, productId);
        List<Long> users = List.of(21L, 22L, 23L);

        // when — 사용자들이 차례로 찜한다
        users.forEach(user -> attempt(() -> productLikeService.like(user, productId)));

        // then — 읽히지 않는 찜 행이 하나도 쌓이지 않는다
        assertThat(productLikeService.countLikes(productId))
                .as("저장된 찜 행 수 (사용자 %d 명이 시도)", users.size())
                .isZero();
    }

    @DisplayName("[PD-07] 삭제 전에 찜한 상품은 삭제된 뒤에도 찜을 취소할 수 있다 (ADR-0013 정리 허용)")
    @Test
    void unlike_afterDeletion() {
        // given — 찜한 뒤 판매자가 삭제한다
        Long productId = saveProduct();
        productLikeService.like(USER_ID, productId);
        productService.delete(SELLER_ID, productId);

        // when
        productLikeService.unlike(USER_ID, productId);

        // then — 이미 있던 찜을 치울 수 있다
        assertThat(productLikeService.isLiked(USER_ID, productId)).isFalse();
        assertThat(productLikeService.countLikes(productId)).isZero();
    }

    @DisplayName("[PD-08] 삭제된 상품은 관심 목록에 나오지 않는다 (QA-1.5)")
    @Test
    void myLikedProducts_excludesDeleted() {
        // given — 찜한 뒤 판매자가 삭제한다
        Long productId = saveProduct();
        productLikeService.like(USER_ID, productId);
        productService.delete(SELLER_ID, productId);

        // when
        var liked = productLikeService.myLikedProducts(USER_ID);

        // then — 관심 목록에서는 걸러진다. 찜 행은 남아 사용자 눈에만 안 띈다
        assertThat(liked).isEmpty();
        assertThat(productLikeService.isLiked(USER_ID, productId)).isTrue();
    }

    private void attempt(Runnable operation) {
        try {
            operation.run();
        } catch (BusinessException expected) {
        }
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
