package project.kjhjdh.ibid.product.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductViewRedisRepositoryTest extends IntegrationTestSupport {

    private static final Long PRODUCT_ID = 1L;
    private static final Long OTHER_PRODUCT_ID = 2L;
    private static final String VISITOR_ID = "visitor-a";
    private static final String OTHER_VISITOR_ID = "visitor-b";
    private static final Duration DEDUP_TTL = Duration.ofMinutes(30);

    @Autowired
    private ProductViewRedisRepository productViewRedisRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @DisplayName("처음 조회한 방문자는 조회수가 증가하고 반영 대상으로 등록된다")
    @Test
    void recordView() {
        // when
        boolean recorded = productViewRedisRepository.recordView(PRODUCT_ID, VISITOR_ID, DEDUP_TTL);

        // then
        assertThat(recorded).isTrue();
        assertThat(productViewRedisRepository.findPendingCount(PRODUCT_ID)).isEqualTo(1L);
        assertThat(productViewRedisRepository.findPendingProductIds()).containsExactly(PRODUCT_ID);
    }

    @DisplayName("중복 방지 기간 안에 같은 방문자가 다시 조회하면 조회수가 증가하지 않는다")
    @Test
    void recordView_duplicated() {
        // given
        productViewRedisRepository.recordView(PRODUCT_ID, VISITOR_ID, DEDUP_TTL);

        // when
        boolean recorded = productViewRedisRepository.recordView(PRODUCT_ID, VISITOR_ID, DEDUP_TTL);

        // then
        assertThat(recorded).isFalse();
        assertThat(productViewRedisRepository.findPendingCount(PRODUCT_ID)).isEqualTo(1L);
    }

    @DisplayName("다른 방문자가 조회하면 조회수가 증가한다")
    @Test
    void recordView_otherVisitor() {
        // given
        productViewRedisRepository.recordView(PRODUCT_ID, VISITOR_ID, DEDUP_TTL);

        // when
        boolean recorded = productViewRedisRepository.recordView(PRODUCT_ID, OTHER_VISITOR_ID, DEDUP_TTL);

        // then
        assertThat(recorded).isTrue();
        assertThat(productViewRedisRepository.findPendingCount(PRODUCT_ID)).isEqualTo(2L);
    }

    @DisplayName("같은 방문자라도 다른 상품이면 각각 조회수가 증가한다")
    @Test
    void recordView_otherProduct() {
        // given
        productViewRedisRepository.recordView(PRODUCT_ID, VISITOR_ID, DEDUP_TTL);

        // when
        boolean recorded = productViewRedisRepository.recordView(OTHER_PRODUCT_ID, VISITOR_ID, DEDUP_TTL);

        // then
        assertThat(recorded).isTrue();
        assertThat(productViewRedisRepository.findPendingCount(PRODUCT_ID)).isEqualTo(1L);
        assertThat(productViewRedisRepository.findPendingCount(OTHER_PRODUCT_ID)).isEqualTo(1L);
        assertThat(productViewRedisRepository.findPendingProductIds())
                .containsExactlyInAnyOrder(PRODUCT_ID, OTHER_PRODUCT_ID);
    }

    @DisplayName("중복 방지 키에는 설정한 만료 기간이 적용된다")
    @Test
    void recordView_setsDedupTtl() {
        // when
        productViewRedisRepository.recordView(PRODUCT_ID, VISITOR_ID, DEDUP_TTL);

        // then
        Long ttl = stringRedisTemplate.getExpire("product-view::1::visitor-a", TimeUnit.SECONDS);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(DEDUP_TTL.toSeconds());
    }

    @DisplayName("미반영 조회수를 꺼내면 카운터와 반영 대상이 비워진다")
    @Test
    void takePendingCount() {
        // given
        productViewRedisRepository.recordView(PRODUCT_ID, VISITOR_ID, DEDUP_TTL);
        productViewRedisRepository.recordView(PRODUCT_ID, OTHER_VISITOR_ID, DEDUP_TTL);

        // when
        long taken = productViewRedisRepository.takePendingCount(PRODUCT_ID);

        // then
        assertThat(taken).isEqualTo(2L);
        assertThat(productViewRedisRepository.findPendingCount(PRODUCT_ID)).isZero();
        assertThat(productViewRedisRepository.findPendingProductIds()).isEmpty();
    }

    @DisplayName("미반영 조회수가 없으면 0을 반환한다")
    @Test
    void takePendingCount_absent() {
        // when
        long taken = productViewRedisRepository.takePendingCount(PRODUCT_ID);

        // then
        assertThat(taken).isZero();
    }
}
