package project.kjhjdh.ibid.notification.presentation;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.config.RabbitConfig;
import project.kjhjdh.ibid.common.event.NotificationMessage;
import project.kjhjdh.ibid.notification.application.NotificationService;
import project.kjhjdh.ibid.notification.domain.NotificationType;

@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void onNotification(NotificationMessage message) {
        notificationService.save(
                message.recipientId(),
                NotificationType.valueOf(message.type().name()),
                message.referenceId());
    }
}
