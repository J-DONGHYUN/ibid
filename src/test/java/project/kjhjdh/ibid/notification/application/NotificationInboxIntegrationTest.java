package project.kjhjdh.ibid.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.notification.domain.Notification;
import project.kjhjdh.ibid.notification.domain.NotificationType;

import project.kjhjdh.ibid.support.IntegrationTestSupport;

class NotificationInboxIntegrationTest extends IntegrationTestSupport {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;

    @Autowired
    private NotificationService notificationService;

    @DisplayName("[NT-03] 내 알림만 최신순 커서로 조회하고, 이어 받으면 겹침·빠짐이 없다")
    @Test
    void getNotifications_ownOnlyAndCursor() {
        // given — 내 알림 3개 + 남의 알림 1개
        Long n1 = notificationService.save(ME, NotificationType.NEW_MESSAGE, 10L).getId();
        Long n2 = notificationService.save(ME, NotificationType.RESERVED, 11L).getId();
        notificationService.save(OTHER, NotificationType.SOLD, 12L);
        Long n3 = notificationService.save(ME, NotificationType.SOLD, 13L).getId();

        // when — 첫 조회
        Slice<Notification> first = notificationService.getNotifications(ME, null);

        // then — 내 알림만 최신순 (남의 알림 제외)
        assertThat(first.getContent()).extracting(Notification::getId).containsExactly(n3, n2, n1);

        // when — n2 를 커서로 이어 받으면
        Slice<Notification> next = notificationService.getNotifications(ME, n2);

        // then — n2 미만만, 겹침·빠짐 없음
        assertThat(next.getContent()).extracting(Notification::getId).containsExactly(n1);
    }
}
