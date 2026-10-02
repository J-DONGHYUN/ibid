package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatPresenceRegistryTest {

    private static final Long ROOM = 1L;
    private static final Long USER = 10L;

    private final ChatPresenceRegistry registry = new ChatPresenceRegistry();

    @DisplayName("[NT-01] 구독하면 그 방을 보는 중이다")
    @Test
    void subscribe() {
        registry.subscribe("s1", "sub1", USER, ROOM);

        assertThat(registry.isViewing(ROOM, USER)).isTrue();
    }

    @DisplayName("[NT-01] 구독 해제하면 더는 보는 중이 아니다")
    @Test
    void unsubscribe() {
        registry.subscribe("s1", "sub1", USER, ROOM);

        registry.unsubscribe("s1", "sub1");

        assertThat(registry.isViewing(ROOM, USER)).isFalse();
    }

    @DisplayName("[NT-01] 연결이 끊기면 그 세션의 모든 구독이 사라진다")
    @Test
    void disconnect() {
        registry.subscribe("s1", "sub1", USER, ROOM);
        registry.subscribe("s1", "sub2", USER, 2L);
        registry.subscribe("s2", "sub1", 20L, ROOM);

        registry.disconnect("s1");

        assertThat(registry.isViewing(ROOM, USER)).isFalse();
        assertThat(registry.isViewing(2L, USER)).isFalse();
        assertThat(registry.isViewing(ROOM, 20L)).as("다른 세션은 영향 없음").isTrue();
    }

    @DisplayName("[NT-01] 다른 방·다른 사용자는 보는 중이 아니다")
    @Test
    void isViewing_distinguishesRoomAndUser() {
        registry.subscribe("s1", "sub1", USER, ROOM);

        assertThat(registry.isViewing(2L, USER)).as("다른 방").isFalse();
        assertThat(registry.isViewing(ROOM, 99L)).as("다른 사용자").isFalse();
    }
}
