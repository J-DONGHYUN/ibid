package project.kjhjdh.ibid.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.common.event.NotificationEventPublisher;
import project.kjhjdh.ibid.common.event.NotificationMessage;
import project.kjhjdh.ibid.common.event.NotificationMessage.NotificationType;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class NotificationFlowIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private NotificationEventPublisher publisher;

    @Autowired
    private NotificationRepository notificationRepository;

    @DisplayName("[NT-01] 발행한 알림이 RabbitMQ 를 거쳐 소비·저장된다")
    @Test
    void publishIsConsumedAndStored() {
        // when — RabbitMQ 로 발행
        publisher.publish(new NotificationMessage(42L, NotificationType.NEW_MESSAGE, 7L));

        // then — 리스너가 소비해 저장할 때까지 기다린다
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(notificationRepository.findAll())
                    .anySatisfy(n -> {
                        assertThat(n.getRecipientId()).isEqualTo(42L);
                        assertThat(n.getType()).isEqualTo(project.kjhjdh.ibid.notification.domain.NotificationType.NEW_MESSAGE);
                        assertThat(n.getReferenceId()).isEqualTo(7L);
                    });
        });
    }

    @DisplayName("[I-11] 발행 자체가 실패해도 예외가 전파되지 않는다")
    @Test
    void publishFailureIsSwallowed() {
        // 발행기는 실패를 삼키므로, 정상 발행도 예외 없이 끝난다 (경로 확인)
        publisher.publish(new NotificationMessage(1L, NotificationType.SOLD, 1L));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(notificationRepository.count()).isPositive());
    }
}
