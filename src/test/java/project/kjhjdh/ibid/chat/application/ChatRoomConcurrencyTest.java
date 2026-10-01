package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ChatRoomConcurrencyTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final int THREAD_COUNT = 30;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[I-06] 같은 (상품, 구매 희망자) 쌍으로 동시에 열어도 채팅방은 하나만 생긴다")
    @Test
    void open_concurrentRequestsLeaveSingleRoom() throws InterruptedException {
        // given
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000,
                ProductCondition.USED, DeviceSpecFixture.sample());
        Long productId = productRepository.save(product).getId();

        // when
        List<Exception> failures = runConcurrently(THREAD_COUNT, () -> chatRoomService.open(productId, BUYER_ID));

        // then
        assertThat(chatRoomRepository.findByProductIdAndBuyerId(productId, BUYER_ID)).isPresent();
        assertThat(chatRoomRepository.count()).as("남은 채팅방").isEqualTo(1);
        assertThat(failures).as("동시 요청은 멱등으로 기존 방을 돌려주므로 실패가 없다").isEmpty();
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
