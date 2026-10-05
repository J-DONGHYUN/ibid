package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import io.awspring.cloud.s3.S3Template;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductImageConfirmTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final String BUCKET = "ibid-product-images";

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private S3Template s3Template;

    @DisplayName("[PD-02] 업로드 주소만 받고 확정하지 않은 이미지는 상품에 붙지 않는다 (QA-D.1)")
    @Test
    void unconfirmedImageIsNotAttached() throws MalformedURLException {
        // given — 업로드 주소를 발급받는다
        Long productId = saveProduct();
        given(s3Template.createSignedPutURL(anyString(), anyString(), any(Duration.class)))
                .willReturn(URI.create("https://signed.example/upload").toURL());

        // when — 발급만 받고 확정하지 않는다
        List<PresignedUploadResult> issued = productService.generatePresignedUrls(
                SELLER_ID, productId, List.of(new ImagePresignCommand("front.png", "image/png")));

        // then — 주소와 키는 나오지만 상품에는 이미지가 없다
        assertThat(issued).hasSize(1);
        assertThat(issued.get(0).key()).startsWith("products/" + productId + "/");
        assertThat(imageUrlsOf(productId)).isEmpty();
    }

    @DisplayName("[PD-02] 이 상품으로 발급하지 않은 이미지 주소는 확정할 수 없다 (QA-D.1b)")
    @Test
    @Disabled("QA-D.1b 발견 — confirmImages 가 클라이언트가 보낸 주소를 검증 없이 저장한다. 주소 검증 티켓에서 지운다")
    void confirm_rejectsUrlNotIssuedForThisProduct() {
        // given
        Long productId = saveProduct();

        // when & then — 외부 주소나 남의 상품 경로를 보내면 거부해야 한다
        assertThatThrownBy(() -> productService.confirmImages(
                SELLER_ID, productId, List.of("https://evil.example.com/tracker.png")))
                .isInstanceOf(BusinessException.class);
        assertThat(imageUrlsOf(productId)).isEmpty();
    }

    @DisplayName("[PD-02] 내 상품에서 이미지를 지워도 남의 상품 S3 객체는 지워지지 않는다 (QA-D.1c)")
    @Test
    @Disabled("QA-D.1c 발견 — 남의 상품 이미지 주소를 확정한 뒤 지우면 그 S3 객체가 삭제된다. 주소 검증 티켓에서 지운다")
    void deleteImages_doesNotDeleteAnotherProductsObject() {
        // given — 내 상품에 남의 상품 경로의 주소를 확정해 둔다 (현재는 검증 없이 저장된다)
        Long mine = saveProduct();
        Long others = saveProduct();
        String foreignKey = "products/" + others + "/victim.png";
        String foreignUrl = "https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/" + foreignKey;
        productService.confirmImages(SELLER_ID, mine, List.of(foreignUrl));

        // when — 내 상품에서 그 이미지를 지운다
        productService.deleteImages(SELLER_ID, mine, List.of(foreignUrl));

        // then — 남의 상품 경로의 S3 객체는 건드리지 않아야 한다
        then(s3Template).should(never()).deleteObject(BUCKET, foreignKey);
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
