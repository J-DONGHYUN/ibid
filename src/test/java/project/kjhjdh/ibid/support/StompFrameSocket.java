package project.kjhjdh.ibid.support;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public final class StompFrameSocket implements AutoCloseable {

    private static final long CONNECT_TIMEOUT_SECONDS = 10;
    private static final String NUL = "\0";

    private final WebSocketSession session;
    private final List<String> frames;

    private StompFrameSocket(WebSocketSession session, List<String> frames) {
        this.session = session;
        this.frames = frames;
    }

    public static StompFrameSocket connect(int port, String accessToken) throws Exception {
        List<String> frames = new CopyOnWriteArrayList<>();
        WebSocketSession session = new StandardWebSocketClient()
                .execute(new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                        frames.add(message.getPayload());
                    }
                }, new WebSocketHttpHeaders(), URI.create("ws://localhost:" + port + "/ws"))
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        StompFrameSocket socket = new StompFrameSocket(session, frames);
        socket.send("CONNECT\naccept-version:1.2\nAuthorization:Bearer " + accessToken + "\n\n" + NUL);
        return socket;
    }

    public void subscribe(String subscriptionId, String destination) throws IOException {
        send("SUBSCRIBE\nid:" + subscriptionId + "\ndestination:" + destination + "\n\n" + NUL);
    }

    public void closeWithoutDisconnect() throws IOException {
        session.close(CloseStatus.GOING_AWAY);
    }

    public List<String> frames() {
        return List.copyOf(frames);
    }

    public boolean isOpen() {
        return session.isOpen();
    }

    private void send(String frame) throws IOException {
        session.sendMessage(new TextMessage(frame));
    }

    @Override
    public void close() throws IOException {
        if (session.isOpen()) {
            session.close();
        }
    }
}
