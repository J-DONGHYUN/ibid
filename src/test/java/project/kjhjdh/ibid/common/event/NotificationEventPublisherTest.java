package project.kjhjdh.ibid.common.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import project.kjhjdh.ibid.common.config.RabbitConfig;
import project.kjhjdh.ibid.common.event.NotificationMessage.NotificationType;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationEventPublisher publisher;

    @DisplayName("[NT-01] 알림을 익스체인지로 발행한다")
    @Test
    void publish() {
        // given
        NotificationMessage message = new NotificationMessage(1L, NotificationType.NEW_MESSAGE, 5L);

        // when
        publisher.publish(message);

        // then
        then(rabbitTemplate).should().convertAndSend(
                eq(RabbitConfig.NOTIFICATION_EXCHANGE), eq(RabbitConfig.NOTIFICATION_ROUTING_KEY), eq(message));
    }

    @DisplayName("[I-11] 발행이 실패해도 예외를 던지지 않는다")
    @Test
    void publish_swallowsFailure() {
        // given — 브로커 장애
        willThrow(new AmqpException("broker down"))
                .given(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
        NotificationMessage message = new NotificationMessage(1L, NotificationType.NEW_MESSAGE, 5L);

        // when & then — 삼킨다
        assertThatCode(() -> publisher.publish(message)).doesNotThrowAnyException();
    }
}
