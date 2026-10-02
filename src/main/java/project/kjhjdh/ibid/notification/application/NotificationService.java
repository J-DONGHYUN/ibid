package project.kjhjdh.ibid.notification.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.notification.domain.Notification;
import project.kjhjdh.ibid.notification.domain.NotificationType;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int PAGE_SIZE = 20;

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification save(Long recipientId, NotificationType type, Long referenceId) {
        return notificationRepository.save(Notification.create(recipientId, type, referenceId));
    }

    @Transactional(readOnly = true)
    public Slice<Notification> getNotifications(Long userId, Long cursor) {
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        return notificationRepository.findByRecipientIdAndIdLessThanOrderByIdDesc(
                userId, effectiveCursor, PageRequest.of(0, PAGE_SIZE));
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        notification.markRead();
    }
}
