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
    private static final String ISSUED_BASE = "https://ibid-product-images.s3.ap-northeast-2.amazonaws.com/products/";

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
        String first = issuedUrl(productId, "0b9f6c1e-2d3a-4f5b-8c7d-9e0a1b2c3d4e", "jpg");
        String second = issuedUrl(productId, "1c0a7d2f-3e4b-4a6c-9d8e-0f1b2c3d4e5f", "jpg");
        productService.confirmImages(SELLER_ID, productId, List.of(first, second));

        // when
        ProductDetailResult result = productService.getProduct(productId, VISITOR_ID);

        // then
        assertThat(result.viewCount()).isEqualTo(1L);
        assertThat(result.imageUrls()).containsExactly(first, second);
        assertThat(result.deviceSpec().modelName()).isEqualTo("iPhone 13");
        assertThat(result.productCondition()).isEqualTo(ProductCondition.LIKE_NEW);
        assertThat(result.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(result.createdAt()).isNotNull();
    }

    @DisplayName("[PD-03] 목록은 기본으로 거래완료를 포함하고, 제외하면 거래완료가 빠진다")
    @Test
    void getProducts_includeSold() {
        // given — 판매중 하나, 거래완료 하나
        Long onSale = productService.register(SELLER_ID, registerCommand("판매중"));
        Long sold = productService.register(SELLER_ID, registerCommand("거래완료"));
        Product soldProduct = productRepository.findById(sold).orElseThrow();
        soldProduct.complete(99L);
        productRepository.save(soldProduct);

        // when & then — 포함(기본)
        ProductListResult included = productService.getProducts(null, true);
        assertThat(included.slice().getContent()).extracting(Product::getId).contains(onSale, sold);

        // when & then — 제외
        ProductListResult excluded = productService.getProducts(null, false);
        assertThat(excluded.slice().getContent()).extracting(Product::getId).contains(onSale).doesNotContain(sold);
    }

    private ProductRegisterCommand registerCommand(String title) {
        return new ProductRegisterCommand(title, "상태 좋음", 89000, ProductCondition.LIKE_NEW,
                new DeviceSpecCommand(DeviceCategory.SMARTPHONE, "iPhone 13", 90, "본체, 충전기", null));
    }

    private String issuedUrl(Long productId, String uuid, String extension) {
        return ISSUED_BASE + productId + "/" + uuid + "." + extension;
    }
}
