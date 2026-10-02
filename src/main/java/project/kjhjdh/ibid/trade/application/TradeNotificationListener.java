package project.kjhjdh.ibid.trade.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.event.NotificationEventPublisher;
import project.kjhjdh.ibid.common.event.NotificationMessage;

@Component
@RequiredArgsConstructor
public class TradeNotificationListener {

    private final NotificationEventPublisher notificationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTradeNotification(NotificationMessage message) {
        notificationEventPublisher.publish(message);
    }
}
