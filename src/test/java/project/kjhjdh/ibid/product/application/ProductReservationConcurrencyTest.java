package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductReservationConcurrencyTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final int THREAD_COUNT = 20;

    @Autowired
    private ProductReservationService reservationService;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[I-12] 서로 다른 상대로 동시에 예약해도 한 명만 예약된다 (낙관적 락, ADR-0008)")
    @Test
    void reserve_onlyOneWins() throws InterruptedException {
        // given
        Long productId = saveOnSaleProduct();

        // when
        Measurement m = measure("낙관적 락", productId,
                buyerId -> reservationService.reserve(productId, buyerId));

        // then
        Product reserved = productRepository.findById(productId).orElseThrow();
        assertThat(reserved.getStatus()).isEqualTo(ProductStatus.RESERVED);
        assertThat(reserved.getReservedBuyerId()).isNotNull();
        assertThat(m.failures).hasSize(THREAD_COUNT - 1);
    }

    private Long saveOnSaleProduct() {
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000,
                ProductCondition.USED, DeviceSpecFixture.sample());
        return productRepository.save(product).getId();
    }

    private Measurement measure(String label, Long productId, Consumer<Long> reserve) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        ConcurrentLinkedQueue<Exception> failures = new ConcurrentLinkedQueue<>();
        AtomicLong buyerSeq = new AtomicLong(1000);

        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.execute(() -> {
                long buyerId = buyerSeq.incrementAndGet();
                ready.countDown();
                try {
                    start.await();
                    reserve.accept(buyerId);
                } catch (Exception e) {
                    failures.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        long begin = System.nanoTime();
        start.countDown();
        done.await();
        long elapsedMs = (System.nanoTime() - begin) / 1_000_000;
        pool.shutdown();

        List<Exception> collected = List.copyOf(failures);
        String breakdown = collected.stream()
                .collect(Collectors.groupingBy(e -> e.getClass().getSimpleName(), Collectors.counting()))
                .toString();
        System.out.printf("[EXP-01] %s | 스레드 %d | 성공 %d | 실패 %d %s | %dms%n",
                label, THREAD_COUNT, THREAD_COUNT - collected.size(), collected.size(), breakdown, elapsedMs);

        return new Measurement(collected);
    }

    private record Measurement(List<Exception> failures) {
    }
}
