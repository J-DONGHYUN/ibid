package project.kjhjdh.ibid.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
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

    @DisplayName("[NT-03] 본인 알림을 읽음 처리한다")
    @Test
    void markRead() {
        // given
        Notification notification = Notification.create(1L, NotificationType.NEW_MESSAGE, 5L);
        given(notificationRepository.findById(7L)).willReturn(Optional.of(notification));

        // when
        notificationService.markRead(7L, 1L);

        // then
        assertThat(notification.isRead()).isTrue();
    }

    @DisplayName("[NT-03] 남의 알림은 읽음 처리할 수 없다")
    @Test
    void markRead_notOwner() {
        // given
        Notification notification = Notification.create(1L, NotificationType.NEW_MESSAGE, 5L);
        given(notificationRepository.findById(7L)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.markRead(7L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
        assertThat(notification.isRead()).isFalse();
    }

    @DisplayName("[NT-03] 없는 알림은 읽음 처리할 수 없다")
    @Test
    void markRead_notFound() {
        // given
        given(notificationRepository.findById(7L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.markRead(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOTIFICATION_NOT_FOUND.getMessage());
    }
}
