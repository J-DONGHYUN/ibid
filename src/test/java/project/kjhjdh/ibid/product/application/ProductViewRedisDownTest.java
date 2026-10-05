package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;

import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductViewRedisDownTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Duration ACCEPTABLE_LATENCY = Duration.ofSeconds(5);

    @Autowired
    @Qualifier("redisContainer")
    private GenericContainer<?> redisContainer;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[PD-09] Redis 가 내려가 연결이 거부돼도 상품 상세는 빠르게 응답한다 (QA-B.1 연결 거부)")
    @Test
    void getProduct_whileRedisIsDown() {
        // given — 조회수가 DB 에 3 으로 반영된 상품. Redis 컨테이너가 내려간다 (연결 거부)
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        productRepository.increaseViewCount(productId, 3);
        redisContainer.stop();

        // when
        Instant startedAt = Instant.now();
        ProductDetailResult detail = productService.getProduct(productId, "visitor-a");
        Duration elapsed = Duration.between(startedAt, Instant.now());

        // then — 상세는 정상 응답하고 조회수는 DB 값이다. 연결 거부라 빨리 실패해야 한다
        assertThat(detail.productId()).isEqualTo(productId);
        assertThat(detail.viewCount()).isEqualTo(3L);
        assertThat(elapsed).as("Redis 연결 거부 중 상세 응답 시간").isLessThan(ACCEPTABLE_LATENCY);
    }
}
