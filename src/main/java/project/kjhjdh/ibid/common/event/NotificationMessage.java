package project.kjhjdh.ibid.common.event;

public record NotificationMessage(
        Long recipientId,
        NotificationType type,
        Long referenceId
) {

    public enum NotificationType {
        NEW_MESSAGE,
        RESERVED,
        RESERVATION_CANCELED,
        SOLD
    }
}
