package project.kjhjdh.ibid.trade.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.exception.GlobalExceptionHandler;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class OptimisticLockLoserTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final int THREAD_COUNT = 20;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @DisplayName("[I-12] 동시 예약에서 진 요청은 서버 재시도 없이 낙관적 락 충돌(409) 또는 상태 거부로 끝난다 (QA-C.1)")
    @Test
    void reserve_losersFailWithoutServerRetry() throws InterruptedException {
        // given — 판매중 상품과 20명의 채팅 상대
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        List<Long> buyers = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            Long buyerId = 1000L + i;
            buyers.add(buyerId);
            chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, buyerId));
        }
        AtomicInteger attempts = new AtomicInteger();

        // when — 20명이 동시에 예약을 시도한다
        List<Exception> failures = runConcurrently(buyers, buyerId -> {
            attempts.incrementAndGet();
            reservationService.reserve(productId, SELLER_ID, buyerId);
        });

        // then — 요청마다 시도는 한 번뿐이고(서버 재시도 없음), 진 요청은 두 종류 중 하나로 끝난다
        assertThat(attempts.get()).as("서버 쪽 재시도가 없으면 시도 수 == 요청 수").isEqualTo(THREAD_COUNT);
        assertThat(failures).hasSize(THREAD_COUNT - 1);
        long lockConflicts = failures.stream().filter(OptimisticLockingFailureException.class::isInstance).count();
        long stateRejects = failures.stream()
                .filter(BusinessException.class::isInstance)
                .filter(e -> e.getMessage().equals(ErrorCode.PRODUCT_NOT_ON_SALE.getMessage()))
                .count();
        assertThat(lockConflicts + stateRejects).as("예상한 두 종류 밖의 실패 수").isEqualTo(failures.size());
        assertThat(lockConflicts).as("낙관적 락 충돌로 끝난 요청 수(나머지는 먼저 커밋된 뒤 상태 거부)").isGreaterThan(0);

        // and — 락 충돌은 클라이언트에게 409 CONCURRENT_UPDATE 로 전달된다
        HttpStatus status = (HttpStatus) new GlobalExceptionHandler()
                .handleOptimisticLock(new OptimisticLockingFailureException("conflict")).getStatusCode();
        assertThat(status).isEqualTo(HttpStatus.CONFLICT);
    }

    private List<Exception> runConcurrently(List<Long> buyers, java.util.function.Consumer<Long> task)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        ConcurrentLinkedQueue<Exception> failures = new ConcurrentLinkedQueue<>();

        for (Long buyerId : buyers) {
            pool.execute(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.accept(buyerId);
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
