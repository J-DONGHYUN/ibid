package project.kjhjdh.ibid.trade.application;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.common.event.NotificationEventPublisher;
import project.kjhjdh.ibid.common.event.NotificationMessage;
import project.kjhjdh.ibid.common.event.NotificationMessage.NotificationType;

@ExtendWith(MockitoExtension.class)
class TradeNotificationListenerTest {

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private TradeNotificationListener listener;

    @DisplayName("[NT-02] 커밋 후 거래 알림 이벤트를 RabbitMQ 발행기로 넘긴다")
    @Test
    void onTradeNotification() {
        // given
        NotificationMessage message = new NotificationMessage(20L, NotificationType.RESERVED, 1L);

        // when
        listener.onTradeNotification(message);

        // then
        then(notificationEventPublisher).should().publish(message);
    }
}
