package project.kjhjdh.ibid.order.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class OrderConcurrencyTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final int THREAD_COUNT = 30;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Disabled("락이 없는 현재 purchase()에서는 oversell이 발생해 실패한다 (Phase 3 문제 재현용). 락 방식 구현은 별도 테스트로 검증한다.")
    @DisplayName("재고 1개에 다수가 동시 구매하면 락이 없을 때 oversell이 발생한다")
    @Test
    void naivePurchase_causesOversell() throws InterruptedException {
        // given
        Product product = productRepository.save(Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1));

        // when
        ConcurrencyResult result = runConcurrently(THREAD_COUNT, () ->
                orderService.purchase(BUYER_ID, new PurchaseRequest(product.getId(), 1)));

        // then
        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(result.success()).as("성공한 구매 수(=실제 팔린 개수)").isEqualTo(1);
        assertThat(orderRepository.count()).as("저장된 주문 수").isEqualTo(1);
        assertThat(found.getStock()).as("남은 재고").isZero();
    }

    @DisplayName("비관적 락으로 동시 구매하면 재고 수만큼만 팔린다")
    @Test
    void purchasePessimistic_preventsOversell() throws InterruptedException {
        // given
        Product product = productRepository.save(Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1));

        // when
        ConcurrencyResult result = runConcurrently(THREAD_COUNT, () ->
                orderService.purchasePessimistic(BUYER_ID, new PurchaseRequest(product.getId(), 1)));

        // then
        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(result.success()).as("성공한 구매 수").isEqualTo(1);
        assertThat(result.failure()).as("실패한 구매 수").isEqualTo(THREAD_COUNT - 1);
        assertThat(orderRepository.count()).as("저장된 주문 수").isEqualTo(1);
        assertThat(found.getStock()).as("남은 재고").isZero();
    }

    @DisplayName("낙관적 락으로 동시 구매하면 재고 수만큼만 팔린다")
    @Test
    void purchaseOptimistic_preventsOversell() throws InterruptedException {
        // given
        Product product = productRepository.save(Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1));

        // when
        ConcurrencyResult result = runConcurrently(THREAD_COUNT, () ->
                orderService.purchaseOptimistic(BUYER_ID, new PurchaseRequest(product.getId(), 1)));

        // then
        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(result.success()).as("성공한 구매 수").isEqualTo(1);
        assertThat(result.failure()).as("실패한 구매 수").isEqualTo(THREAD_COUNT - 1);
        assertThat(orderRepository.count()).as("저장된 주문 수").isEqualTo(1);
        assertThat(found.getStock()).as("남은 재고").isZero();
    }

    private ConcurrencyResult runConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.execute(() -> {
                try {
                    task.run();
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        done.await();
        pool.shutdown();

        return new ConcurrencyResult(success.get(), failure.get());
    }

    private record ConcurrencyResult(int success, int failure) {
    }
}
