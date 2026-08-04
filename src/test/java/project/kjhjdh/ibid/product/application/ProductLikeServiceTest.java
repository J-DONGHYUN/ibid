package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductLike;
import project.kjhjdh.ibid.product.infra.ProductLikeRepository;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.product.presentation.dto.ProductSummaryResponse;

@ExtendWith(MockitoExtension.class)
class ProductLikeServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    @Mock
    private ProductLikeRepository productLikeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageService productImageService;

    @InjectMocks
    private ProductLikeService productLikeService;

    private Product product(Long id, String title) {
        Product product = Product.create(1L, title, "설명", 1000, 1);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @DisplayName("찜하지 않은 상품을 찜하면 저장한다")
    @Test
    void like() {
        // given
        given(productRepository.existsById(PRODUCT_ID)).willReturn(true);
        given(productLikeRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(false);

        // when
        productLikeService.like(USER_ID, PRODUCT_ID);

        // then
        then(productLikeRepository).should().save(any(ProductLike.class));
    }

    @DisplayName("이미 찜한 상품을 다시 찜해도 중복 저장하지 않는다")
    @Test
    void like_alreadyLiked() {
        // given
        given(productRepository.existsById(PRODUCT_ID)).willReturn(true);
        given(productLikeRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(true);

        // when
        productLikeService.like(USER_ID, PRODUCT_ID);

        // then
        then(productLikeRepository).should(never()).save(any());
    }

    @DisplayName("존재하지 않는 상품을 찜하면 실패한다")
    @Test
    void like_productNotFound() {
        // given
        given(productRepository.existsById(PRODUCT_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> productLikeService.like(USER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        then(productLikeRepository).shouldHaveNoInteractions();
    }

    @DisplayName("찜을 취소하면 찜 기록을 삭제한다")
    @Test
    void unlike() {
        // when
        productLikeService.unlike(USER_ID, PRODUCT_ID);

        // then
        then(productLikeRepository).should().deleteByUserIdAndProductId(USER_ID, PRODUCT_ID);
    }

    @DisplayName("내 찜 여부를 조회한다")
    @Test
    void isLiked() {
        // given
        given(productLikeRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).willReturn(true);

        // when & then
        assertThat(productLikeService.isLiked(USER_ID, PRODUCT_ID)).isTrue();
    }

    @DisplayName("상품의 찜 수를 조회한다")
    @Test
    void countLikes() {
        // given
        given(productLikeRepository.countByProductId(PRODUCT_ID)).willReturn(5L);

        // when & then
        assertThat(productLikeService.countLikes(PRODUCT_ID)).isEqualTo(5L);
    }

    @DisplayName("관심목록을 찜한 최신순으로 상품 요약(썸네일 포함)으로 반환한다")
    @Test
    void myLikedProducts() {
        // given: 찜 최신순 = 상품10, 상품20
        given(productLikeRepository.findByUserIdOrderByIdDesc(USER_ID)).willReturn(List.of(
                ProductLike.of(USER_ID, 10L), ProductLike.of(USER_ID, 20L)));
        given(productRepository.findAllById(List.of(10L, 20L)))
                .willReturn(List.of(product(20L, "B"), product(10L, "A")));   // 순서 뒤섞여 반환
        given(productImageService.findThumbnails(List.of(10L, 20L)))
                .willReturn(Map.of(10L, "https://t10.jpg"));                  // 20은 썸네일 없음

        // when
        List<ProductSummaryResponse> result = productLikeService.myLikedProducts(USER_ID);

        // then: 찜 순서(10, 20) 유지 + 썸네일 매핑
        assertThat(result).extracting(ProductSummaryResponse::productId).containsExactly(10L, 20L);
        assertThat(result.get(0).thumbnailUrl()).isEqualTo("https://t10.jpg");
        assertThat(result.get(1).thumbnailUrl()).isNull();
    }
}
