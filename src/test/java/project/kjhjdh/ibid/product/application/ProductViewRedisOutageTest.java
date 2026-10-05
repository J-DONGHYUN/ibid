package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;

import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "spring.data.redis.timeout=500ms")
class ProductViewRedisOutageTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;

    @Autowired
    @Qualifier("redisContainer")
    private GenericContainer<?> redisContainer;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductViewCounter productViewCounter;

    @Autowired
    private ProductViewCountFlusher productViewCountFlusher;

    @Autowired
    private ProductRepository productRepository;

    private boolean paused;

    @AfterEach
    void unpauseRedis() {
        if (paused) {
            resumeRedis();
        }
    }

    @DisplayName("[PD-09] Redis 가 응답하지 않아도 상품 상세는 응답하고 조회수만 집계되지 않는다 (QA-B.1)")
    @Test
    void getProduct_whileRedisIsUnresponsive() {
        // given — 조회수가 DB 에 3 으로 반영된 상품. Redis 가 멈춘다
        Long productId = saveProduct();
        productRepository.increaseViewCount(productId, 3);
        pauseRedis();

        // when
        Instant startedAt = Instant.now();
        ProductDetailResult detail = productService.getProduct(productId, "visitor-a");
        Duration elapsed = Duration.between(startedAt, Instant.now());

        // then — 상세는 정상 응답하고 조회수는 DB 값이다. 집계는 비어 있다
        assertThat(detail.productId()).isEqualTo(productId);
        assertThat(detail.viewCount()).isEqualTo(3L);
        assertThat(elapsed).as("Redis 무응답 중 상세 응답 시간 (타임아웃 500ms 설정)").isLessThan(Duration.ofSeconds(5));
    }

    @DisplayName("[PD-09] 플러시 경로는 Redis 장애를 삼키지 않고 그대로 던진다 (QA-B.2 — 현재 동작)")
    @Test
    void flush_propagatesRedisFailure() {
        // given — 미반영 조회수가 있고 Redis 가 멈춘다
        Long productId = saveProduct();
        productViewCounter.record(productId, "visitor-a");
        pauseRedis();

        // when & then — recordView · findPendingCount 와 달리 findPendingProductIds 는 DataAccessException 을 삼키지 않는다
        assertThatThrownBy(() -> productViewCountFlusher.flush()).isInstanceOf(DataAccessException.class);
    }

    @DisplayName("[PD-09] Redis 장애 중 플러시가 실패해도 스케줄러는 살아 있고 복구 뒤 조회수를 반영한다 (QA-B.2)")
    @Test
    void scheduler_survivesRedisOutage() {
        // given — 미반영 조회수 3 건과 반복 실행되는 플러시. 스프링 스케줄러처럼 반복 작업의 예외를 로그로만 남긴다
        Long productId = saveProduct();
        productViewCounter.record(productId, "visitor-a");
        productViewCounter.record(productId, "visitor-b");
        productViewCounter.record(productId, "visitor-c");
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        ProductViewCountScheduler flushTask = new ProductViewCountScheduler(productViewCountFlusher);
        scheduler.scheduleWithFixedDelay(flushTask::flushViewCounts, Duration.ofMillis(200));

        try {
            // when — Redis 가 멈춘 채 여러 주기가 실패하고, 다시 살아난다
            pauseRedis();
            sleep(Duration.ofSeconds(2));
            resumeRedis();

            // then — 같은 스케줄러가 계속 돌아 복구 뒤 조회수를 DB 에 반영한다
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                    assertThat(productRepository.findById(productId).orElseThrow().getViewCount()).isEqualTo(3L));
        } finally {
            scheduler.shutdown();
        }
    }

    @DisplayName("[PD-09] Redis 가 복구되면 집계가 이어지고, 무응답 중 보낸 기록은 해동 뒤 적용될 수 있다 (QA-B.3)")
    @Test
    void recording_resumesAfterRedisRecovers() {
        // given — 복구 전에 2 건(a · b) 기록
        Long productId = saveProduct();
        productViewCounter.record(productId, "visitor-a");
        productViewCounter.record(productId, "visitor-b");

        // when — 무응답 중 2 건(c · d)을 보내고 복구된 뒤 1 건(e)을 기록한다
        pauseRedis();
        productViewCounter.record(productId, "visitor-c");
        productViewCounter.record(productId, "visitor-d");
        resumeRedis();
        productViewCounter.record(productId, "visitor-e");
        productViewCountFlusher.flush();

        // then — 복구 전후 3 건(a · b · e)은 반드시 반영된다. pause 는 서버를 얼리기만 해서 클라이언트가 타임아웃으로
        //        포기한 c · d 가 해동 뒤 서버에서 실행될 수 있다(0~2 건). 연결 거부와 달리 "유실" 을 재현하지 못하는 주입 방식이다
        assertThat(productRepository.findById(productId).orElseThrow().getViewCount()).isBetween(3L, 5L);
    }

    @DisplayName("[PD-09] 플러시 중 DB 반영이 실패해도 꺼낸 조회수가 사라지지 않는다 (QA-B.4)")
    @Test
    @Disabled("QA-B.4 발견 — Redis 에서 꺼낸 뒤 DB 반영이 실패하면 조회수가 사라진다(기대 3, 실제 0). 꺼내기와 반영의 순서를 고치는 티켓에서 지운다")
    void flush_doesNotLoseViewsWhenDatabaseFails() {
        // given — 미반영 조회수 3 건. DB 반영이 한 번 실패하는 플러셔
        Long productId = saveProduct();
        productViewCounter.record(productId, "visitor-a");
        productViewCounter.record(productId, "visitor-b");
        productViewCounter.record(productId, "visitor-c");
        ProductRepository failingRepository = mock(ProductRepository.class);
        given(failingRepository.increaseViewCount(productId, 3L)).willThrow(new QueryTimeoutException("db down"));
        ProductViewCountFlusher failingFlusher = new ProductViewCountFlusher(productViewCounter, failingRepository);

        // when — 반영이 실패한 플러시 뒤에 정상 플러시가 돈다
        assertThatThrownBy(failingFlusher::flush).isInstanceOf(QueryTimeoutException.class);
        productViewCountFlusher.flush();

        // then — 실패한 주기의 조회수도 다음 주기에 반영돼야 한다
        assertThat(productRepository.findById(productId).orElseThrow().getViewCount()).isEqualTo(3L);
    }

    private void pauseRedis() {
        redisContainer.getDockerClient().pauseContainerCmd(redisContainer.getContainerId()).exec();
        paused = true;
    }

    private void resumeRedis() {
        redisContainer.getDockerClient().unpauseContainerCmd(redisContainer.getContainerId()).exec();
        paused = false;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
