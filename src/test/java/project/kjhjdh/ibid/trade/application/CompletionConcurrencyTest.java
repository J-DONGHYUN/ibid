package project.kjhjdh.ibid.trade.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class CompletionConcurrencyTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CompletionService completionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @DisplayName("[I-12] 예약 해제와 거래완료가 동시에 들어와도 상태가 어긋나지 않는다 (낙관적 락, ADR-0008)")
    @Test
    void cancelAndCompleteConcurrently() throws InterruptedException {
        // given — 예약중 상품(그 상대에게 채팅방이 있어 I-04 통과)
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID));
        reservationService.reserve(productId, SELLER_ID, BUYER_ID);

        // when — 예약 해제와 거래완료가 동시에
        List<Exception> failures = runConcurrently(
                () -> reservationService.cancelReservation(productId, SELLER_ID),
                () -> completionService.complete(productId, SELLER_ID, BUYER_ID));

        // then — 최종 상태는 두 전이 중 하나의 일관된 결과다 (찢어지지 않는다)
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(failures).as("적어도 한 쪽은 RESERVED 에서 성공하므로 실패는 0 또는 1").hasSizeLessThanOrEqualTo(1);
        if (product.getStatus() == ProductStatus.SOLD) {
            assertThat(product.getSoldBuyerId()).isEqualTo(BUYER_ID);
            assertThat(product.getReservedBuyerId()).as("거래완료면 예약 상대 없음").isNull();
        } else {
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
            assertThat(product.getReservedBuyerId()).as("해제면 예약 상대 없음").isNull();
            assertThat(product.getSoldBuyerId()).as("해제면 거래 상대 없음").isNull();
        }
    }

    private List<Exception> runConcurrently(Runnable... tasks) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.length);
        CountDownLatch ready = new CountDownLatch(tasks.length);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tasks.length);
        ConcurrentLinkedQueue<Exception> failures = new ConcurrentLinkedQueue<>();

        for (Runnable task : tasks) {
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
