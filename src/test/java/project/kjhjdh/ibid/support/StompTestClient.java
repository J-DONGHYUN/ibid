package project.kjhjdh.ibid.support;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public final class StompTestClient implements AutoCloseable {

    private static final long CONNECT_TIMEOUT_SECONDS = 10;
    private static final long SUBSCRIBE_TIMEOUT_SECONDS = 5;

    private final WebSocketStompClient client;
    private final ThreadPoolTaskScheduler scheduler;
    private final StompSession session;
    private final List<Map<String, Object>> received = new CopyOnWriteArrayList<>();
    private final List<String> errors;

    private StompTestClient(WebSocketStompClient client, ThreadPoolTaskScheduler scheduler,
                            StompSession session, List<String> errors) {
        this.client = client;
        this.scheduler = scheduler;
        this.session = session;
        this.errors = errors;
    }

    public static StompTestClient connect(int port, String accessToken) throws Exception {
        List<String> errors = new CopyOnWriteArrayList<>();
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        client.setTaskScheduler(scheduler);
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + accessToken);
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                errors.add(String.valueOf(headers.getFirst("message")));
                            }

                            @Override
                            public void handleTransportError(StompSession session, Throwable exception) {
                                errors.add(exception.toString());
                            }
                        })
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        session.setAutoReceipt(true);
        return new StompTestClient(client, scheduler, session, errors);
    }

    public boolean subscribe(String destination) throws InterruptedException {
        CountDownLatch accepted = new CountDownLatch(1);
        try {
            StompSession.Subscription subscription = session.subscribe(destination, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                @SuppressWarnings("unchecked")
                public void handleFrame(StompHeaders headers, Object payload) {
                    received.add((Map<String, Object>) payload);
                }
            });
            subscription.addReceiptTask(accepted::countDown);
        } catch (IllegalStateException connectionClosed) {
            return false;
        }
        return accepted.await(SUBSCRIBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public List<Map<String, Object>> received() {
        return List.copyOf(received);
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    @Override
    public void close() {
        if (session.isConnected()) {
            session.disconnect();
        }
        client.stop();
        scheduler.shutdown();
    }
}
