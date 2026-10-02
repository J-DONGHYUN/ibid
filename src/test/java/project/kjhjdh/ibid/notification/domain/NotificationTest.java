package project.kjhjdh.ibid.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @DisplayName("[NT-03] 알림을 읽음 처리하면 읽음 상태가 된다")
    @Test
    void markRead() {
        // given
        Notification notification = Notification.create(10L, NotificationType.NEW_MESSAGE, 5L);
        assertThat(notification.isRead()).isFalse();

        // when
        notification.markRead();

        // then
        assertThat(notification.isRead()).isTrue();
    }

    @DisplayName("[NT-03] 이미 읽은 알림을 다시 읽음 처리해도 읽음 상태다 (멱등)")
    @Test
    void markRead_idempotent() {
        // given
        Notification notification = Notification.create(10L, NotificationType.NEW_MESSAGE, 5L);
        notification.markRead();

        // when
        notification.markRead();

        // then
        assertThat(notification.isRead()).isTrue();
    }

    @DisplayName("[NT-03] 수신자 본인 여부를 판별한다")
    @Test
    void isOwnedBy() {
        // given
        Notification notification = Notification.create(10L, NotificationType.NEW_MESSAGE, 5L);

        // when & then
        assertThat(notification.isOwnedBy(10L)).isTrue();
        assertThat(notification.isOwnedBy(99L)).isFalse();
    }
}
