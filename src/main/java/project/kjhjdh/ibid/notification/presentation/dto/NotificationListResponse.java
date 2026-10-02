package project.kjhjdh.ibid.notification.presentation.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.notification.domain.Notification;

public record NotificationListResponse(
        List<NotificationResponse> notifications,
        Long nextCursor,
        boolean hasNext
) {

    public static NotificationListResponse of(Slice<Notification> slice) {
        List<Notification> content = slice.getContent();
        Long nextCursor = slice.hasNext() && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;
        return new NotificationListResponse(
                content.stream().map(NotificationResponse::from).toList(),
                nextCursor,
                slice.hasNext());
    }
}
