package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductLikeRepository;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductLikeConcurrencyTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long USER_ID = 1L;
    private static final int THREAD_COUNT = 30;

    @Autowired
    private ProductLikeService productLikeService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductLikeRepository productLikeRepository;

    @DisplayName("같은 사용자가 동시에 찜을 요청해도 유니크 제약으로 찜 기록은 1건만 남는다")
    @Test
    void like_concurrentRequestsLeaveSingleLike() throws InterruptedException {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
        Long productId = productRepository.save(product).getId();

        // when
        List<Exception> failures = runConcurrently(THREAD_COUNT, () -> productLikeService.like(USER_ID, productId));

        // then
        assertThat(productLikeRepository.countByProductId(productId)).as("남은 찜 기록").isEqualTo(1);
        assertThat(failures).as("동시 요청 실패 원인")
                .allMatch(DataIntegrityViolationException.class::isInstance);
    }

    private List<Exception> runConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Exception> failures = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            pool.execute(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.run();
                } catch (Exception e) {
                    failures.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        pool.shutdown();

        return List.copyOf(failures);
    }
}
