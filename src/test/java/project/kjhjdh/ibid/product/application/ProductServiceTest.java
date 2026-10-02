package project.kjhjdh.ibid.product.application;

import project.kjhjdh.ibid.product.DeviceSpecFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.product.domain.DeviceCategory;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;

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

    @Mock
    private ProductImageService productImageService;

    @InjectMocks
    private ProductService productService;

    @DisplayName("본인 상품이면 이미지 서비스가 발급한 presigned URL을 그대로 응답한다")
    @Test
    void generatePresignedUrls() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        List<PresignedUploadResult> presigned = List.of(
                new PresignedUploadResult("https://presigned1", "products/1/a.jpg", "https://image1"),
                new PresignedUploadResult("https://presigned2", "products/1/b.png", "https://image2"));
        given(productImageService.presign(eq(PRODUCT_ID), any())).willReturn(presigned);
        List<ImagePresignCommand> commands = List.of(
                new ImagePresignCommand("a.jpg", "image/jpeg"),
                new ImagePresignCommand("b.png", "image/png"));

        // when
        List<PresignedUploadResult> result = productService.generatePresignedUrls(SELLER_ID, PRODUCT_ID, commands);

        // then
        assertThat(result).isEqualTo(presigned);
    }

    @DisplayName("본인 상품이 아니면 presigned URL을 발급할 수 없다")
    @Test
    void generatePresignedUrls_notOwner() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
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
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.confirmImages(SELLER_ID, PRODUCT_ID,
                List.of("https://image1.jpg", "https://image2.png"));

        // then
        assertThat(product.imageUrls()).containsExactly("https://image1.jpg", "https://image2.png");
    }

    @DisplayName("본인 상품이 아니면 이미지 URL을 저장할 수 없다")
    @Test
    void confirmImages_notOwner() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.confirmImages(OTHER_USER_ID, PRODUCT_ID,
                List.of("https://image1")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
        then(productImageService).shouldHaveNoInteractions();
    }

    @DisplayName("본인 상품이면 상품 정보를 수정한다")
    @Test
    void update() {
        // given
        Product product = Product.create(SELLER_ID, "예전 제목", "예전 설명", 1000, ProductCondition.USED, DeviceSpecFixture.sample());
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.update(SELLER_ID, PRODUCT_ID,
                new ProductUpdateCommand("새 제목", "새 설명", 50000, ProductCondition.NEW,
                        new DeviceSpecCommand(DeviceCategory.LAPTOP, "MacBook Air M2", 80, "본체", null)));

        // then
        assertThat(product.getTitle()).isEqualTo("새 제목");
        assertThat(product.getPrice()).isEqualTo(50000);
        assertThat(product.getProductCondition()).isEqualTo(ProductCondition.NEW);
        assertThat(product.getDeviceSpec().modelName()).isEqualTo("MacBook Air M2");
    }

    @DisplayName("본인 상품이 아니면 수정할 수 없다")
    @Test
    void update_notOwner() {
        // given
        Product product = Product.create(SELLER_ID, "예전 제목", "예전 설명", 1000, ProductCondition.USED, DeviceSpecFixture.sample());
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.update(OTHER_USER_ID, PRODUCT_ID,
                new ProductUpdateCommand("새 제목", "새 설명", 50000, ProductCondition.NEW,
                        new DeviceSpecCommand(DeviceCategory.LAPTOP, "MacBook Air M2", 80, "본체", null))))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("선택한 이미지를 상품에서 제거하고 S3 삭제를 위임한다")
    @Test
    void deleteImages() {
        // given
        Product product = Product.create(SELLER_ID, "나이키", "설명", 1000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.addImages(List.of("https://img1.jpg", "https://img2.png", "https://img3.gif"));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.deleteImages(SELLER_ID, PRODUCT_ID, List.of("https://img1.jpg", "https://img2.png"));

        // then
        assertThat(product.imageUrls()).containsExactly("https://img3.gif");
        then(productImageService).should().deleteFiles(List.of("https://img1.jpg", "https://img2.png"));
    }

    @DisplayName("[PD-06] 본인 상품을 삭제하면 소프트 삭제되고 행·이미지는 남는다")
    @Test
    void delete() {
        // given
        Product product = Product.create(SELLER_ID, "나이키", "설명", 1000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.addImages(List.of("https://img1.jpg", "https://img2.png"));
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        productService.delete(SELLER_ID, PRODUCT_ID);

        // then — 소프트 삭제: deletedAt 만 찍히고 행·S3 는 그대로
        assertThat(product.isDeleted()).isTrue();
        then(productRepository).should(never()).delete(any(Product.class));
        then(productImageService).should(never()).deleteFiles(any());
    }

    @DisplayName("[PD-06] 이미 삭제된 상품은 다시 삭제할 수 없다")
    @Test
    void delete_alreadyDeleted() {
        // given
        Product product = Product.create(SELLER_ID, "나이키", "설명", 1000, ProductCondition.USED, DeviceSpecFixture.sample());
        product.delete();
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then — findOwnedProduct 가 삭제 상품을 404 로 거른다
        assertThatThrownBy(() -> productService.delete(SELLER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @DisplayName("본인 상품이 아니면 삭제할 수 없다")
    @Test
    void delete_notOwner() {
        // given
        Product product = Product.create(SELLER_ID, "나이키", "설명", 1000, ProductCondition.USED, DeviceSpecFixture.sample());
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.delete(OTHER_USER_ID, PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
        then(productImageService).shouldHaveNoInteractions();
    }

    @DisplayName("상품을 상세 조회하면 조회가 기록되고 집계된 조회수가 응답된다")
    @Test
    void getProduct() {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample());
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        given(productViewCounter.readTotal(product)).willReturn(164L);

        // when
        ProductDetailResult result = productService.getProduct(PRODUCT_ID, VISITOR_ID);

        // then
        assertThat(result.viewCount()).isEqualTo(164L);
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

    @DisplayName("[CH-05] 상품 요약을 id 로 묶어 제목과 썸네일과 함께 돌려준다")
    @Test
    void findSummaries() {
        // given
        Product product = Product.create(SELLER_ID, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample());
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        given(productImageService.findThumbnails(List.of(PRODUCT_ID))).willReturn(Map.of(PRODUCT_ID, "https://thumb"));
        given(productRepository.findAllById(List.of(PRODUCT_ID))).willReturn(List.of(product));

        // when
        Map<Long, ProductSummary> result = productService.findSummaries(List.of(PRODUCT_ID));

        // then
        assertThat(result).containsOnlyKeys(PRODUCT_ID);
        assertThat(result.get(PRODUCT_ID).title()).isEqualTo("아이폰 13");
        assertThat(result.get(PRODUCT_ID).thumbnailUrl()).isEqualTo("https://thumb");
    }
}
