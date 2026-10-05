package project.kjhjdh.ibid.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import project.kjhjdh.ibid.common.config.RabbitConfig;
import project.kjhjdh.ibid.common.event.NotificationMessage;
import project.kjhjdh.ibid.common.event.NotificationMessage.NotificationType;
import project.kjhjdh.ibid.notification.application.NotificationService;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationPoisonMessageTest extends IntegrationTestSupport {

    private static final Long POISON_RECIPIENT = 1L;
    private static final Long HEALTHY_RECIPIENT = 2L;
    private static final int MAX_ACCEPTABLE_ATTEMPTS = 5;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private NotificationService notificationService;

    @DisplayName("[I-11] 처리 중 예외가 나는 알림은 무한 재배달되지 않는다 (QA-A.1)")
    @Test
    @Disabled("QA-A.1 발견 — 소비 예외가 무한 재배달된다(3초에 11705회). T-44 에서 DLQ · 재시도 상한을 도입하고 이 줄을 지운다")
    void poisonMessageIsNotRedeliveredForever() throws InterruptedException {
        // given — 이 수신자의 알림은 저장 중 항상 예외가 난다 (DB 장애 · 배포 어긋남으로 인한 값 등)
        willThrow(new IllegalStateException("poison")).given(notificationService)
                .save(eq(POISON_RECIPIENT), any(), any());

        // when — 알림 한 건을 발행하고 몇 초 둔다
        rabbitTemplate.convertAndSend(RabbitConfig.NOTIFICATION_EXCHANGE, RabbitConfig.NOTIFICATION_ROUTING_KEY,
                new NotificationMessage(POISON_RECIPIENT, NotificationType.NEW_MESSAGE, 7L));
        Thread.sleep(3000);

        // then — 재시도 상한이 있다면 몇 번 시도하다 DLQ 로 빠진다. 상한이 없으면 시도 횟수가 계속 는다
        int attempts = mockingDetails(notificationService).getInvocations().size();
        assertThat(attempts)
                .as("3초 동안 한 건에 대한 소비 시도 횟수")
                .isLessThanOrEqualTo(MAX_ACCEPTABLE_ATTEMPTS);
    }

    @DisplayName("[I-11] 독약 알림이 있어도 뒤따르는 정상 알림은 처리된다 (QA-A.1 선두 막힘)")
    @Test
    void healthyMessageIsProcessedBehindPoison() {
        // given
        willThrow(new IllegalStateException("poison")).given(notificationService)
                .save(eq(POISON_RECIPIENT), any(), any());

        // when — 독약 알림 뒤에 정상 알림을 발행한다
        rabbitTemplate.convertAndSend(RabbitConfig.NOTIFICATION_EXCHANGE, RabbitConfig.NOTIFICATION_ROUTING_KEY,
                new NotificationMessage(POISON_RECIPIENT, NotificationType.NEW_MESSAGE, 7L));
        rabbitTemplate.convertAndSend(RabbitConfig.NOTIFICATION_EXCHANGE, RabbitConfig.NOTIFICATION_ROUTING_KEY,
                new NotificationMessage(HEALTHY_RECIPIENT, NotificationType.NEW_MESSAGE, 8L));

        // then — 정상 알림이 독약에 막히지 않고 소비된다
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                verify(notificationService, atLeastOnce()).save(eq(HEALTHY_RECIPIENT), any(), any()));
    }
}
