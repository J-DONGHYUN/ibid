package project.kjhjdh.ibid.trade.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
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

class ReservationConcurrencyTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final int THREAD_COUNT = 20;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @DisplayName("[I-12] 서로 다른 상대로 동시에 예약해도 한 명만 예약된다 (낙관적 락, ADR-0008)")
    @Test
    void reserve_onlyOneWins() throws InterruptedException {
        // given — 판매중 상품과 20명의 채팅 상대(각자 채팅방이 있어 I-04 통과)
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000, ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        List<Long> buyers = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            Long buyerId = 1000L + i;
            buyers.add(buyerId);
            chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, buyerId));
        }

        // when — 20명이 동시에 예약을 시도한다
        List<Exception> failures = runConcurrently(buyers,
                buyerId -> reservationService.reserve(productId, SELLER_ID, buyerId));

        // then — 한 명만 예약되고 나머지는 실패한다
        Product reserved = productRepository.findById(productId).orElseThrow();
        assertThat(reserved.getStatus()).isEqualTo(ProductStatus.RESERVED);
        assertThat(reserved.getReservedBuyerId()).isIn(buyers);
        assertThat(failures).as("동시 예약에서 진 요청들").hasSize(THREAD_COUNT - 1);
    }

    private List<Exception> runConcurrently(List<Long> buyers, java.util.function.Consumer<Long> reserve)
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
                    reserve.accept(buyerId);
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
