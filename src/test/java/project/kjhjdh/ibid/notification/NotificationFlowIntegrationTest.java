package project.kjhjdh.ibid.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import project.kjhjdh.ibid.common.event.NotificationMessage;
import project.kjhjdh.ibid.common.event.NotificationMessage.NotificationType;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class NotificationFlowIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @DisplayName("[NT-01] 트랜잭션이 없는 경로에서 낸 알림 이벤트도 저장된다 (ADR-0014 fallbackExecution)")
    @Test
    void eventWithoutTransactionIsStored() {
        // when — 채팅 메시지 전송처럼 트랜잭션 없이 이벤트를 낸다
        eventPublisher.publishEvent(new NotificationMessage(42L, NotificationType.NEW_MESSAGE, 7L));

        // then — 커밋을 기다릴 트랜잭션이 없으므로 즉시 저장된다
        assertThat(notificationRepository.findAll()).anySatisfy(n -> {
            assertThat(n.getRecipientId()).isEqualTo(42L);
            assertThat(n.getType()).isEqualTo(project.kjhjdh.ibid.notification.domain.NotificationType.NEW_MESSAGE);
            assertThat(n.getReferenceId()).isEqualTo(7L);
        });
    }

    @DisplayName("[NT-02] 트랜잭션이 있는 경로의 알림은 커밋된 뒤에 저장된다")
    @Test
    void eventInTransactionIsStoredAfterCommit() {
        // when — 거래 유스케이스처럼 트랜잭션 안에서 이벤트를 낸다
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new NotificationMessage(43L, NotificationType.SOLD, 8L)));

        // then — 커밋 후 리스너가 저장한다
        assertThat(notificationRepository.findAll()).anySatisfy(n -> {
            assertThat(n.getRecipientId()).isEqualTo(43L);
            assertThat(n.getReferenceId()).isEqualTo(8L);
        });
    }

    @DisplayName("[I-11] 롤백된 트랜잭션의 알림은 저장되지 않는다")
    @Test
    void eventInRolledBackTransactionIsNotStored() {
        // when — 이벤트를 낸 트랜잭션이 롤백된다
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new NotificationMessage(44L, NotificationType.RESERVED, 9L));
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);

        // then — 없던 일의 알림은 나가지 않는다
        assertThat(notificationRepository.findAll())
                .noneSatisfy(n -> assertThat(n.getRecipientId()).isEqualTo(44L));
    }
}
