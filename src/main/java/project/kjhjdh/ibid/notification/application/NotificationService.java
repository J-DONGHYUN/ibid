package project.kjhjdh.ibid.notification.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.notification.domain.Notification;
import project.kjhjdh.ibid.notification.domain.NotificationType;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification save(Long recipientId, NotificationType type, Long referenceId) {
        return notificationRepository.save(Notification.create(recipientId, type, referenceId));
    }
}
