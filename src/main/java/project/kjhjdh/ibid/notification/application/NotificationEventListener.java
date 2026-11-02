package project.kjhjdh.ibid.notification.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.kjhjdh.ibid.common.event.NotificationMessage;
import project.kjhjdh.ibid.notification.domain.NotificationType;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(NotificationMessage message) {
        try {
            notificationService.save(
                    message.recipientId(),
                    NotificationType.valueOf(message.type().name()),
                    message.referenceId());
        } catch (RuntimeException e) {
            log.warn("알림 저장 실패 — 원 작업은 영향받지 않는다 (I-11): {}", message, e);
        }
    }
}
