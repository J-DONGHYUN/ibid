package project.kjhjdh.ibid.common.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.kjhjdh.ibid.common.config.RabbitConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(NotificationMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.NOTIFICATION_EXCHANGE, RabbitConfig.NOTIFICATION_ROUTING_KEY, message);
        } catch (RuntimeException e) {
            log.warn("알림 발행 실패 — 원 작업은 영향받지 않는다 (I-11): {}", message, e);
        }
    }
}
