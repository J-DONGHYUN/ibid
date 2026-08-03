package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.common.image.infra.S3ImageUploader;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductImage;
import project.kjhjdh.ibid.product.infra.ProductImageRepository;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignResponse;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    private static final Long PRODUCT_ID = 1L;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private S3ImageUploader s3ImageUploader;

    @InjectMocks
    private ProductImageService productImageService;

    @Captor
    private ArgumentCaptor<List<ProductImage>> imagesCaptor;

    private Product product(Long id) {
        Product product = Product.create(10L, "상품", "설명", 1000, 1);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ProductImage productImage(Long productId, String url, int sortOrder) {
        return ProductImage.of(product(productId), url, sortOrder);
    }

    @DisplayName("파일명·타입으로 presigned URL을 발급해 응답으로 매핑한다")
    @Test
    void presign() {
        // given
        given(s3ImageUploader.generatePresignedUrl(any(), any(), any()))
                .willReturn(new PresignedUploadResult("https://presigned", "products/1/uuid.jpg", "https://image"));

        // when
        List<ImagePresignResponse> result = productImageService.presign(PRODUCT_ID,
                List.of(new ImagePresignRequest("a.jpg", "image/jpeg")));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).presignedUrl()).isEqualTo("https://presigned");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://image");
    }

    @DisplayName("기존 이미지 뒤에 이어서 sortOrder를 매겨 저장한다")
    @Test
    void attach() {
        // given
        Product product = product(PRODUCT_ID);
        given(productImageRepository.countByProductId(PRODUCT_ID)).willReturn(2);

        // when
        productImageService.attach(product, List.of("https://img/a.jpg", "https://img/b.png"));

        // then
        then(productImageRepository).should().saveAll(imagesCaptor.capture());
        List<ProductImage> saved = imagesCaptor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getSortOrder()).isEqualTo(2);
        assertThat(saved.get(1).getSortOrder()).isEqualTo(3);
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

    @DisplayName("선택한 이미지를 DB와 S3에서 함께 삭제한다")
    @Test
    void deleteByUrls() {
        // given
        List<ProductImage> images = List.of(
                productImage(PRODUCT_ID, "https://img/a.jpg", 0),
                productImage(PRODUCT_ID, "https://img/b.jpg", 1)
        );
        given(productImageRepository.findByProductIdAndUrlIn(PRODUCT_ID,
                List.of("https://img/a.jpg", "https://img/b.jpg"))).willReturn(images);

        // when
        productImageService.deleteByUrls(PRODUCT_ID, List.of("https://img/a.jpg", "https://img/b.jpg"));

        // then
        then(productImageRepository).should().deleteAll(images);
        then(s3ImageUploader).should(times(2)).delete(anyString());
    }

    @DisplayName("상품의 모든 이미지를 DB와 S3에서 삭제한다")
    @Test
    void deleteAll() {
        // given
        List<ProductImage> images = List.of(
                productImage(PRODUCT_ID, "https://img/a.jpg", 0),
                productImage(PRODUCT_ID, "https://img/b.jpg", 1)
        );
        given(productImageRepository.findByProductId(PRODUCT_ID)).willReturn(images);

        // when
        productImageService.deleteAll(PRODUCT_ID);

        // then
        then(productImageRepository).should().deleteAll(images);
        then(s3ImageUploader).should(times(2)).delete(anyString());
    }
}
