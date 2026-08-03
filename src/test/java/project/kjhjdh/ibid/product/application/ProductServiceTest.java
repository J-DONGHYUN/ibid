package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
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
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.product.presentation.dto.ImageConfirmRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductUpdateRequest;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageService productImageService;

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

    @DisplayName("본인 상품이면 이미지 서비스가 발급한 presigned URL을 그대로 응답한다")
    @Test
    void generatePresignedUrls() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        List<ImagePresignResponse> presigned = List.of(
                new ImagePresignResponse("https://presigned1", "products/1/a.jpg", "https://image1"),
                new ImagePresignResponse("https://presigned2", "products/1/b.png", "https://image2"));
        given(productImageService.presign(eq(PRODUCT_ID), any())).willReturn(presigned);
        List<ImagePresignRequest> requests = List.of(
                new ImagePresignRequest("a.jpg", "image/jpeg"),
                new ImagePresignRequest("b.png", "image/png"));

        // when
        List<ImagePresignResponse> result = productService.generatePresignedUrls(SELLER_ID, PRODUCT_ID, requests);

        // then
        assertThat(result).isEqualTo(presigned);
    }

    @DisplayName("본인 상품이 아니면 presigned URL을 발급할 수 없다")
    @Test
    void generatePresignedUrls_notOwner() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.generatePresignedUrls(OTHER_USER_ID, PRODUCT_ID, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("본인 상품이면 업로드된 이미지 URL 저장을 이미지 서비스에 위임한다")
    @Test
    void confirmImages() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.confirmImages(SELLER_ID, PRODUCT_ID,
                new ImageConfirmRequest(List.of("https://image1.jpg", "https://image2.png")));

        // then
        assertThat(product.imageUrls()).containsExactly("https://image1.jpg", "https://image2.png");
    }

    @DisplayName("본인 상품이 아니면 이미지 URL을 저장할 수 없다")
    @Test
    void confirmImages_notOwner() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.confirmImages(OTHER_USER_ID, PRODUCT_ID,
                new ImageConfirmRequest(List.of("https://image1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
        then(productImageService).shouldHaveNoInteractions();
    }

    @DisplayName("본인 상품이면 상품 정보를 수정한다")
    @Test
    void update() {
        // given
        Product product = Product.create(SELLER_ID, "예전 제목", "예전 설명", 1000, 1);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.update(SELLER_ID, PRODUCT_ID,
                new ProductUpdateRequest("새 제목", "새 설명", 50000, 5, ProductCondition.NEW));

        // then
        assertThat(product.getTitle()).isEqualTo("새 제목");
        assertThat(product.getPrice()).isEqualTo(50000);
        assertThat(product.getStock()).isEqualTo(5);
        assertThat(product.getProductCondition()).isEqualTo(ProductCondition.NEW);
    }

    @DisplayName("본인 상품이 아니면 수정할 수 없다")
    @Test
    void update_notOwner() {
        // given
        Product product = Product.create(SELLER_ID, "예전 제목", "예전 설명", 1000, 1);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.update(OTHER_USER_ID, PRODUCT_ID,
                new ProductUpdateRequest("새 제목", "새 설명", 50000, 5, ProductCondition.NEW)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("선택한 이미지를 상품에서 제거하고 S3 삭제를 위임한다")
    @Test
    void deleteImages() {
        // given
        Product product = Product.create(SELLER_ID, "나이키", "설명", 1000, 1);
        product.addImages(List.of("https://img1.jpg", "https://img2.png", "https://img3.gif"));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.deleteImages(SELLER_ID, PRODUCT_ID, List.of("https://img1.jpg", "https://img2.png"));

        // then
        assertThat(product.imageUrls()).containsExactly("https://img3.gif");
        then(productImageService).should().deleteFiles(List.of("https://img1.jpg", "https://img2.png"));
    }

    @DisplayName("본인 상품을 삭제하면 상품과 이미지(DB·S3)를 함께 삭제한다")
    @Test
    void delete() {
        // given
        Product product = Product.create(SELLER_ID, "나이키", "설명", 1000, 1);
        product.addImages(List.of("https://img1.jpg", "https://img2.png"));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.delete(SELLER_ID, PRODUCT_ID);

        // then
        then(productRepository).should().delete(product);
        then(productImageService).should().deleteFiles(List.of("https://img1.jpg", "https://img2.png"));
    }

    @DisplayName("본인 상품이 아니면 삭제할 수 없다")
    @Test
    void delete_notOwner() {
        // given
        Product product = Product.create(SELLER_ID, "나이키", "설명", 1000, 1);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.delete(OTHER_USER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
        then(productImageService).shouldHaveNoInteractions();
    }
}
