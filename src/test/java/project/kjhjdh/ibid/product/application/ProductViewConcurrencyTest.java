package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductViewConcurrencyTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final int THREAD_COUNT = 30;

    @Autowired
    private ProductViewCounter productViewCounter;

    @Autowired
    private ProductViewCountFlusher productViewCountFlusher;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("서로 다른 방문자가 동시에 조회하면 조회수가 하나도 유실되지 않는다")
    @Test
    void record_losesNoViewUnderConcurrency() throws InterruptedException {
        // given
        Long productId = saveProduct();

        // when
        runConcurrently(index -> productViewCounter.record(productId, "visitor-" + index));
        productViewCountFlusher.flush();

        // then
        assertThat(findViewCount(productId)).as("반영된 조회수").isEqualTo(THREAD_COUNT);
    }

    @DisplayName("같은 방문자가 동시에 여러 번 조회해도 조회수는 1만 증가한다")
    @Test
    void record_countsOnceUnderConcurrency() throws InterruptedException {
        // given
        Long productId = saveProduct();

        // when
        runConcurrently(index -> productViewCounter.record(productId, "visitor-a"));
        productViewCountFlusher.flush();

        // then
        assertThat(findViewCount(productId)).as("반영된 조회수").isEqualTo(1L);
    }

    private void runConcurrently(IntConsumer task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            int index = i;
            pool.execute(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.accept(index);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        pool.shutdown();
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 3)).getId();
    }

    private long findViewCount(Long productId) {
        return productRepository.findById(productId).orElseThrow().getViewCount();
    }
}
