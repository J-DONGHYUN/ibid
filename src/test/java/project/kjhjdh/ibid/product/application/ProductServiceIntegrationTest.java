package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.domain.DeviceCategory;
import project.kjhjdh.ibid.product.domain.DeviceSpec;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductServiceIntegrationTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final String VISITOR_ID = "visitor-a";

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[PD-01] 전자기기 정보를 함께 등록하면 그 값이 상품에 저장된다")
    @Test
    void register_storesDeviceSpec() {
        // given
        ProductRegisterCommand command = new ProductRegisterCommand(
                "아이폰 13", "상태 좋음", 500000, ProductCondition.LIKE_NEW,
                new DeviceSpecCommand(DeviceCategory.SMARTPHONE, "iPhone 13", 87, "본체, 케이블", "액정 잔기스"));

        // when
        Long productId = productService.register(SELLER_ID, command);

        // then
        Product saved = productRepository.findById(productId).orElseThrow();
        DeviceSpec spec = saved.getDeviceSpec();
        assertThat(spec.category()).isEqualTo(DeviceCategory.SMARTPHONE);
        assertThat(spec.modelName()).isEqualTo("iPhone 13");
        assertThat(spec.batteryHealth()).isEqualTo(87);
        assertThat(spec.components()).isEqualTo("본체, 케이블");
        assertThat(spec.defects()).isEqualTo("액정 잔기스");
        assertThat(saved.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @DisplayName("[PD-01] 하자를 비워도 등록된다")
    @Test
    void register_allowsBlankDefects() {
        // given
        ProductRegisterCommand command = new ProductRegisterCommand(
                "맥북", "상태 좋음", 900000, ProductCondition.USED,
                new DeviceSpecCommand(DeviceCategory.LAPTOP, "MacBook Air M2", 92, "본체, 충전기", null));

        // when
        Long productId = productService.register(SELLER_ID, command);

        // then
        assertThat(productRepository.findById(productId)).get()
                .extracting(product -> product.getDeviceSpec().defects())
                .isNull();
    }

    @DisplayName("상품 상세 조회는 조회수·이미지·상품상태를 한 응답에 모두 담는다")
    @Test
    void getProduct_returnsEveryDetailField() {
        // given
        Long productId = productService.register(SELLER_ID, registerCommand("나이키 후드"));
        productService.confirmImages(SELLER_ID, productId,
                List.of("https://image/a.jpg", "https://image/b.jpg"));

        // when
        ProductDetailResult result = productService.getProduct(productId, VISITOR_ID);

        // then
        assertThat(result.viewCount()).isEqualTo(1L);
        assertThat(result.imageUrls()).containsExactly("https://image/a.jpg", "https://image/b.jpg");
        assertThat(result.productCondition()).isEqualTo(ProductCondition.LIKE_NEW);
        assertThat(result.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(result.createdAt()).isNotNull();
    }

    private ProductRegisterCommand registerCommand(String title) {
        return new ProductRegisterCommand(title, "상태 좋음", 89000, ProductCondition.LIKE_NEW,
                new DeviceSpecCommand(DeviceCategory.SMARTPHONE, "iPhone 13", 90, "본체, 충전기", null));
    }
}
