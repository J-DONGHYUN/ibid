package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductViewRedisRepository;

@ExtendWith(MockitoExtension.class)
class ProductViewCounterTest {

    private static final Long PRODUCT_ID = 1L;
    private static final String VISITOR_ID = "visitor-a";
    private static final Duration DEDUP_TTL = Duration.ofMinutes(30);

    @Mock
    private ProductViewRedisRepository productViewRedisRepository;

    private ProductViewCounter productViewCounter;

    @BeforeEach
    void setUp() {
        productViewCounter = new ProductViewCounter(productViewRedisRepository, DEDUP_TTL);
    }

    @DisplayName("조회를 기록할 때 설정된 중복 방지 기간을 그대로 전달한다")
    @Test
    void record() {
        // when
        productViewCounter.record(PRODUCT_ID, VISITOR_ID);

        // then
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        then(productViewRedisRepository).should()
                .recordView(eq(PRODUCT_ID), eq(VISITOR_ID), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(DEDUP_TTL);
    }

    @DisplayName("표시용 조회수는 반영된 누적값과 미반영 조회수의 합이다")
    @Test
    void readTotal() {
        // given
        Product product = persistedProduct(100L);
        given(productViewRedisRepository.findPendingCount(PRODUCT_ID)).willReturn(7L);

        // when
        long total = productViewCounter.readTotal(product);

        // then
        assertThat(total).isEqualTo(107L);
    }

    @DisplayName("미반영 조회수가 없으면 반영된 누적값만 노출한다")
    @Test
    void readTotal_noPendingCount() {
        // given
        Product product = persistedProduct(100L);
        given(productViewRedisRepository.findPendingCount(PRODUCT_ID)).willReturn(0L);

        // when
        long total = productViewCounter.readTotal(product);

        // then
        assertThat(total).isEqualTo(100L);
    }

    private Product persistedProduct(long viewCount) {
        Product product = Product.create(10L, "나이키 후드", "상태 좋음", 89000, 3);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        ReflectionTestUtils.setField(product, "viewCount", viewCount);
        return product;
    }
}
