package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import io.awspring.cloud.s3.S3Template;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductImageConfirmTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final String BUCKET = "ibid-product-images";
    private static final String HOST = "https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com";
    private static final long OTHER_PRODUCT_OFFSET = 1000L;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private S3Template s3Template;

    static Stream<Arguments> notIssuedUrls() {
        return Stream.of(
                Arguments.of("외부 도메인", "https://evil.example.com/tracker.png"),
                Arguments.of("다른 상품 경로", "{host}/products/{other}/{uuid}.png"),
                Arguments.of("다른 버킷", "https://other-bucket.s3.ap-northeast-2.amazonaws.com/products/{id}/{uuid}.png"),
                Arguments.of("경로 탈출", "{host}/products/{id}/../{other}/{uuid}.png"),
                Arguments.of("하위 경로", "{host}/products/{id}/sub/{uuid}.png"),
                Arguments.of("허용되지 않은 확장자", "{host}/products/{id}/{uuid}.exe"),
                Arguments.of("쿼리스트링 덧붙임", "{host}/products/{id}/{uuid}.png?x=1"),
                Arguments.of("http 주소", "http://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/products/{id}/{uuid}.png"));
    }

    @DisplayName("[PD-02] 업로드 주소만 받고 확정하지 않은 이미지는 상품에 붙지 않는다 (QA-D.1)")
    @Test
    void unconfirmedImageIsNotAttached() throws MalformedURLException {
        // given
        Long productId = saveProduct();
        givenSignedUrl();

        // when
        List<PresignedUploadResult> issued = productService.generatePresignedUrls(
                SELLER_ID, productId, List.of(new ImagePresignCommand("front.png", "image/png")));

        // then
        assertThat(issued).hasSize(1);
        assertThat(issued.get(0).key()).startsWith("products/" + productId + "/");
        assertThat(imageUrlsOf(productId)).isEmpty();
    }

    @DisplayName("[PD-02] 이 상품으로 발급한 주소는 확정할 수 있다 (QA-D.1b 과차단 방지)")
    @Test
    void issuedUrlCanBeConfirmed() throws MalformedURLException {
        // given
        Long productId = saveProduct();
        givenSignedUrl();
        String issuedUrl = productService.generatePresignedUrls(
                SELLER_ID, productId, List.of(new ImagePresignCommand("front.png", "image/png"))).get(0).imageUrl();

        // when
        productService.confirmImages(SELLER_ID, productId, List.of(issuedUrl));

        // then
        assertThat(imageUrlsOf(productId)).containsExactly(issuedUrl);
    }

    @DisplayName("[PD-02] 이 상품으로 발급하지 않은 주소는 확정할 수 없고 저장되지 않는다 (QA-D.1b)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("notIssuedUrls")
    void notIssuedUrlIsRejected(String variant, String template) {
        // given
        Long productId = saveProduct();
        String url = fill(template, productId);

        // when
        Throwable thrown = catchThrowable(() -> productService.confirmImages(SELLER_ID, productId, List.of(url)));

        // then — 거부되고 아무것도 저장되지 않는다
        assertThat(imageUrlsOf(productId)).as("저장된 주소 (%s)", variant).isEmpty();
        assertThat(thrown).isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_IMAGE_URL.getMessage());
    }

    @DisplayName("[PD-02] 이미 저장돼 있던 다른 상품 경로 주소를 지워도 그 S3 객체 삭제는 호출되지 않는다 (QA-D.1c)")
    @Test
    void deleteImages_doesNotDeleteAnotherProductsObject() {
        // given — 검증이 없던 시절에 내 상품에 붙어 버린 남의 상품 경로 주소 (옛 데이터)
        Long mine = saveProduct();
        Long others = saveProduct();
        String foreignKey = "products/" + others + "/" + UUID.randomUUID() + ".png";
        String foreignUrl = HOST + "/" + foreignKey;
        attachDirectly(mine, foreignUrl);

        // when
        productService.deleteImages(SELLER_ID, mine, List.of(foreignUrl));

        // then — 상품에서는 떼지만 남의 상품 경로의 S3 객체는 건드리지 않는다
        assertThat(imageUrlsOf(mine)).isEmpty();
        then(s3Template).should(never()).deleteObject(BUCKET, foreignKey);
    }

    @DisplayName("[PD-02] 내 상품에 발급한 이미지를 지우면 그 S3 객체 삭제가 호출된다 (QA-D.1c 과차단 방지)")
    @Test
    void deleteImages_deletesOwnObject() {
        // given
        Long productId = saveProduct();
        String ownKey = "products/" + productId + "/" + UUID.randomUUID() + ".png";
        String ownUrl = HOST + "/" + ownKey;
        attachDirectly(productId, ownUrl);

        // when
        productService.deleteImages(SELLER_ID, productId, List.of(ownUrl));

        // then
        assertThat(imageUrlsOf(productId)).isEmpty();
        then(s3Template).should().deleteObject(BUCKET, ownKey);
    }

    private String fill(String template, Long productId) {
        return template
                .replace("{host}", HOST)
                .replace("{id}", String.valueOf(productId))
                .replace("{other}", String.valueOf(productId + OTHER_PRODUCT_OFFSET))
                .replace("{uuid}", UUID.randomUUID().toString());
    }

    private void givenSignedUrl() throws MalformedURLException {
        given(s3Template.createSignedPutURL(anyString(), anyString(), any(Duration.class)))
                .willReturn(URI.create("https://signed.example/upload").toURL());
    }

    private void attachDirectly(Long productId, String url) {
        transactionTemplate.executeWithoutResult(status -> {
            Product product = productRepository.findById(productId).orElseThrow();
            product.addImages(List.of(url));
            productRepository.save(product);
        });
    }

    private List<String> imageUrlsOf(Long productId) {
        return transactionTemplate.execute(status ->
                productRepository.findById(productId).orElseThrow().imageUrls());
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
