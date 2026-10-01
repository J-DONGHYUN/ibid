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

import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatMessageRepository;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ChatMessageConcurrencyTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final String CLIENT_MSG_ID = "c-1";
    private static final int THREAD_COUNT = 30;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @DisplayName("[I-07] 같은 클라이언트 메시지 식별자로 동시에 보내도 메시지는 하나만 저장된다")
    @Test
    void send_concurrentRequestsLeaveSingleMessage() throws InterruptedException {
        // given
        Long roomId = chatRoomRepository.save(ChatRoom.open(99L, SELLER_ID, BUYER_ID)).getId();

        // when
        List<Exception> failures = runConcurrently(THREAD_COUNT,
                () -> chatMessageService.send(new SendMessageCommand(roomId, BUYER_ID, CLIENT_MSG_ID, "안녕하세요")));

        // then
        assertThat(chatMessageRepository.count()).as("남은 메시지").isEqualTo(1);
        assertThat(failures).as("동시 요청은 멱등으로 기존 메시지를 돌려주므로 실패가 없다").isEmpty();
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
