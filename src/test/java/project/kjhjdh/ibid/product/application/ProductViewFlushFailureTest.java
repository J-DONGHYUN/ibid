package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;

import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductViewFlushFailureTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;

    @Autowired
    private ProductViewCounter productViewCounter;

    @Autowired
    private ProductViewCountFlusher productViewCountFlusher;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[PD-09] 여러 상품 중 하나의 DB 반영이 실패해도 실패한 상품의 조회수는 남아 다음 주기에 반영된다 (QA-B.4)")
    @Test
    void flush_failureOfOneProduct_keepsItsViews() {
        // given — 미반영 조회수가 있는 상품 둘. 한 상품의 DB 반영만 실패하는 플러셔
        Long failing = saveProduct();
        Long healthy = saveProduct();
        record(failing, 3, "f");
        record(healthy, 2, "h");
        ProductViewCountFlusher failingFlusher = new ProductViewCountFlusher(productViewCounter, failingFor(failing));

        // when — 실패한 플러시 뒤에 정상 플러시가 돈다
        try {
            failingFlusher.flush();
        } catch (QueryTimeoutException expected) {
        }
        productViewCountFlusher.flush();

        // then — 어느 상품의 조회수도 사라지지 않는다
        assertThat(viewCountOf(failing)).as("실패한 상품의 최종 조회수 (기대 3)").isEqualTo(3L);
        assertThat(viewCountOf(healthy)).as("정상 상품의 최종 조회수 (기대 2)").isEqualTo(2L);
    }

    @DisplayName("[PD-09] 실패한 플러시가 조회수를 되돌려 놓는 동안 들어오는 새 조회와 섞여도 합이 맞다 (QA-B.4 동시성)")
    @Test
    void flush_failureWhileNewViewsArrive_keepsTotal() throws Exception {
        // given — 미반영 조회수 3 건과, 플러시가 실패하는 동안 계속 들어오는 새 조회 40 건(방문자는 모두 다르다)
        Long productId = saveProduct();
        record(productId, 3, "before");
        ProductViewCountFlusher failingFlusher = new ProductViewCountFlusher(productViewCounter, failingFor(productId));
        int newViews = 40;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < newViews; i++) {
            String visitor = "during-" + i;
            futures.add(executor.submit(() -> {
                start.await();
                productViewCounter.record(productId, visitor);
                return null;
            }));
        }
        futures.add(executor.submit(() -> {
            start.await();
            for (int i = 0; i < 10; i++) {
                try {
                    failingFlusher.flush();
                } catch (QueryTimeoutException expected) {
                }
            }
            return null;
        }));

        // when — 새 조회와 실패한 플러시가 겹친 뒤 정상 플러시가 돈다
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();
        productViewCountFlusher.flush();

        // then — 기록한 3 + 40 건이 하나도 사라지거나 두 번 세이지 않고 반영된다
        assertThat(viewCountOf(productId)).as("최종 조회수 (기대 %d)", 3 + newViews).isEqualTo(3L + newViews);
    }

    private ProductRepository failingFor(Long productId) {
        ProductRepository repository = mock(ProductRepository.class);
        given(repository.increaseViewCount(anyLong(), anyLong())).willAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (id.equals(productId)) {
                throw new QueryTimeoutException("db down");
            }
            return productRepository.increaseViewCount(id, invocation.getArgument(1));
        });
        return repository;
    }

    private void record(Long productId, int count, String visitorPrefix) {
        for (int i = 0; i < count; i++) {
            productViewCounter.record(productId, visitorPrefix + "-" + i);
        }
    }

    private long viewCountOf(Long productId) {
        return productRepository.findById(productId).orElseThrow().getViewCount();
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
