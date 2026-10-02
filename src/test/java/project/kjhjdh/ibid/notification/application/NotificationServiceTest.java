package project.kjhjdh.ibid.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.notification.domain.Notification;
import project.kjhjdh.ibid.notification.domain.NotificationType;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @DisplayName("[NT-01] 수신자·종류·참조로 알림을 저장한다")
    @Test
    void save() {
        // given
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Notification saved = notificationService.save(1L, NotificationType.NEW_MESSAGE, 5L);

        // then
        assertThat(saved.getRecipientId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo(NotificationType.NEW_MESSAGE);
        assertThat(saved.getReferenceId()).isEqualTo(5L);
        assertThat(saved.isRead()).isFalse();
    }
}
