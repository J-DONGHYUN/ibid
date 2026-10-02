package project.kjhjdh.ibid.notification.presentation;

import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.notification.application.NotificationService;
import project.kjhjdh.ibid.notification.domain.Notification;
import project.kjhjdh.ibid.notification.presentation.dto.NotificationListResponse;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationListResponse> list(
            @LoginUser UserInfo loginUser,
            @RequestParam(required = false) Long cursor
    ) {
        Slice<Notification> slice = notificationService.getNotifications(loginUser.userId(), cursor);
        return ResponseEntity.ok(NotificationListResponse.of(slice));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> read(
            @LoginUser UserInfo loginUser,
            @PathVariable Long notificationId
    ) {
        notificationService.markRead(notificationId, loginUser.userId());
        return ResponseEntity.ok().build();
    }
}
