package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.product.infra.s3.PresignedUploadResult;
import project.kjhjdh.ibid.product.infra.s3.S3ImageUploader;
import project.kjhjdh.ibid.product.presentation.dto.ImageConfirmRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignResponse;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private S3ImageUploader s3ImageUploader;

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

    @DisplayName("본인 상품이면 요청한 이미지 수만큼 presigned URL을 발급한다")
    @Test
    void generatePresignedUrls() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        given(s3ImageUploader.generatePresignedUrl(any(), any(), any()))
                .willReturn(new PresignedUploadResult("https://presigned", "products/1/uuid.jpg", "https://image"));
        List<ImagePresignRequest> requests = List.of(
                new ImagePresignRequest("a.jpg", "image/jpeg"),
                new ImagePresignRequest("b.png", "image/png"));

        // when
        List<ImagePresignResponse> result = productService.generatePresignedUrls(SELLER_ID, PRODUCT_ID, requests);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).presignedUrl()).isEqualTo("https://presigned");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://image");
    }

    @DisplayName("존재하지 않는 상품에 presigned URL을 요청하면 실패한다")
    @Test
    void generatePresignedUrls_notFound() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.generatePresignedUrls(SELLER_ID, PRODUCT_ID, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
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

    @DisplayName("본인 상품이면 업로드된 이미지 URL을 상품에 저장한다")
    @Test
    void confirmImages() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.confirmImages(SELLER_ID, PRODUCT_ID,
                new ImageConfirmRequest(List.of("https://image1", "https://image2")));

        // then
        assertThat(product.getImageUrls()).containsExactly("https://image1", "https://image2");
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
        assertThat(product.getImageUrls()).isEmpty();
    }
}
