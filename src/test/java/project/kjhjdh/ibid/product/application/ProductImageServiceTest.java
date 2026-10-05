package project.kjhjdh.ibid.product.application;

import project.kjhjdh.ibid.product.domain.ProductCondition;

import project.kjhjdh.ibid.product.DeviceSpecFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.common.image.infra.S3ImageUploader;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductImage;
import project.kjhjdh.ibid.product.infra.ProductImageRepository;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    private static final Long PRODUCT_ID = 1L;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private S3ImageUploader s3ImageUploader;

    @InjectMocks
    private ProductImageService productImageService;

    private ProductImage productImage(Long productId, String url, int sortOrder) {
        Product product = Product.create(10L, "상품", "설명", 1000, ProductCondition.USED, DeviceSpecFixture.sample());
        ReflectionTestUtils.setField(product, "id", productId);
        return ProductImage.of(product, url, sortOrder);
    }

    @DisplayName("파일명으로 presigned URL을 발급해 업로드 결과로 돌려준다")
    @Test
    void presign() {
        // given
        given(s3ImageUploader.generatePresignedUrl(any(), any()))
                .willReturn(new PresignedUploadResult("https://presigned", "products/1/uuid.jpg", "https://image"));

        // when
        List<PresignedUploadResult> result = productImageService.presign(PRODUCT_ID,
                List.of(new ImagePresignCommand("a.jpg", "image/jpeg")));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).presignedUrl()).isEqualTo("https://presigned");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://image");
    }

    @DisplayName("상품별 첫 이미지를 대표 이미지로 매핑한다")
    @Test
    void findThumbnails() {
        // given
        given(productImageRepository.findByProductIdInOrderBySortOrder(List.of(1L, 2L)))
                .willReturn(List.of(
                        productImage(1L, "https://img/1a.jpg", 0),
                        productImage(1L, "https://img/1b.jpg", 1),
                        productImage(2L, "https://img/2a.png", 0)
                ));

        // when
        Map<Long, String> thumbnails = productImageService.findThumbnails(List.of(1L, 2L));

        // then
        assertThat(thumbnails).containsEntry(1L, "https://img/1a.jpg");
        assertThat(thumbnails).containsEntry(2L, "https://img/2a.png");
    }

    @DisplayName("빈 상품 목록이면 조회 없이 빈 맵을 반환한다")
    @Test
    void findThumbnails_empty() {
        // when
        Map<Long, String> thumbnails = productImageService.findThumbnails(List.of());

        // then
        assertThat(thumbnails).isEmpty();
    }

    @DisplayName("이 상품으로 발급한 URL들을 S3에서 삭제한다")
    @Test
    void deleteFiles() {
        // given
        given(s3ImageUploader.isIssuedUrl("products/1", "https://img/a.jpg")).willReturn(true);
        given(s3ImageUploader.isIssuedUrl("products/1", "https://img/b.jpg")).willReturn(true);

        // when
        productImageService.deleteFiles(PRODUCT_ID, List.of("https://img/a.jpg", "https://img/b.jpg"));

        // then
        then(s3ImageUploader).should(times(2)).delete(anyString());
    }

    @DisplayName("[PD-02] 이 상품으로 발급하지 않은 URL 은 S3 에서 삭제하지 않는다")
    @Test
    void deleteFiles_skipsNotIssued() {
        // given
        given(s3ImageUploader.isIssuedUrl("products/1", "https://img/mine.jpg")).willReturn(true);
        given(s3ImageUploader.isIssuedUrl("products/1", "https://img/foreign.jpg")).willReturn(false);

        // when
        productImageService.deleteFiles(PRODUCT_ID, List.of("https://img/mine.jpg", "https://img/foreign.jpg"));

        // then
        then(s3ImageUploader).should().delete("https://img/mine.jpg");
        then(s3ImageUploader).should(never()).delete("https://img/foreign.jpg");
    }

    @DisplayName("[PD-02] 모두 이 상품으로 발급한 URL 이면 통과한다")
    @Test
    void requireIssued() {
        // given
        given(s3ImageUploader.isIssuedUrl("products/1", "https://img/a.jpg")).willReturn(true);

        // when & then
        assertThatCode(() -> productImageService.requireIssued(PRODUCT_ID, List.of("https://img/a.jpg")))
                .doesNotThrowAnyException();
    }

    @DisplayName("[PD-02] 하나라도 이 상품으로 발급하지 않은 URL 이면 INVALID_IMAGE_URL 이다")
    @Test
    void requireIssued_notIssued() {
        // given
        given(s3ImageUploader.isIssuedUrl("products/1", "https://img/a.jpg")).willReturn(true);
        given(s3ImageUploader.isIssuedUrl("products/1", "https://evil/b.jpg")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> productImageService.requireIssued(PRODUCT_ID,
                List.of("https://img/a.jpg", "https://evil/b.jpg")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_IMAGE_URL.getMessage());
    }
}
