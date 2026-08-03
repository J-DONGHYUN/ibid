package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.product.presentation.dto.ProductDetailResponse;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;

    private static final String VISITOR_ID = "visitor-a";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductViewCounter productViewCounter;

    @InjectMocks
    private ProductService productService;

    @DisplayName("판매자가 판매를 시작하면 상품이 판매중 상태가 된다")
    @Test
    void openForSale() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.openForSale(SELLER_ID, PRODUCT_ID);

        // then
        assertThat(product.isOnSale()).isTrue();
    }

    @DisplayName("존재하지 않는 상품의 판매를 시작하면 실패한다")
    @Test
    void openForSale_notFound() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.openForSale(SELLER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @DisplayName("등록한 판매자가 아니면 판매를 시작할 수 없다")
    @Test
    void openForSale_notOwner() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.openForSale(OTHER_USER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
        assertThat(product.isOnSale()).isFalse();
    }

    @DisplayName("판매 대기 상태가 아닌 상품은 판매를 시작할 수 없다")
    @Test
    void openForSale_notPending() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        product.openForSale();
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.openForSale(SELLER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_PENDING.getMessage());
    }

    @DisplayName("상품을 상세 조회하면 조회가 기록되고 집계된 조회수가 응답된다")
    @Test
    void getProduct() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        given(productViewCounter.readTotal(product)).willReturn(164L);

        // when
        ProductDetailResponse response = productService.getProduct(PRODUCT_ID, VISITOR_ID);

        // then
        assertThat(response.viewCount()).isEqualTo(164L);
        then(productViewCounter).should().record(PRODUCT_ID, VISITOR_ID);
    }

    @DisplayName("존재하지 않는 상품을 상세 조회하면 조회가 기록되지 않는다")
    @Test
    void getProduct_notFound() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(PRODUCT_ID, VISITOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        then(productViewCounter).shouldHaveNoInteractions();
    }
}
