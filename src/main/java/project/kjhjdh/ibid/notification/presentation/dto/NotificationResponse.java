package project.kjhjdh.ibid.notification.presentation.dto;

import java.time.LocalDateTime;

import project.kjhjdh.ibid.notification.domain.Notification;
import project.kjhjdh.ibid.notification.domain.NotificationType;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        Long referenceId,
        boolean read,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
