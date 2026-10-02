package project.kjhjdh.ibid.chat.application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ChatPresenceRegistry {

    private record Subscription(Long userId, Long chatRoomId) {
    }

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    public void subscribe(String sessionId, String subscriptionId, Long userId, Long chatRoomId) {
        subscriptions.put(key(sessionId, subscriptionId), new Subscription(userId, chatRoomId));
    }

    public void unsubscribe(String sessionId, String subscriptionId) {
        subscriptions.remove(key(sessionId, subscriptionId));
    }

    public void disconnect(String sessionId) {
        String prefix = sessionId + ":";
        subscriptions.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public boolean isViewing(Long chatRoomId, Long userId) {
        return subscriptions.values().stream()
                .anyMatch(s -> s.chatRoomId().equals(chatRoomId) && s.userId().equals(userId));
    }

    private String key(String sessionId, String subscriptionId) {
        return sessionId + ":" + subscriptionId;
    }
}
