package project.kjhjdh.ibid.notification.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Notification(Long recipientId, NotificationType type, Long referenceId) {
        this.recipientId = recipientId;
        this.type = type;
        this.referenceId = referenceId;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    public static Notification create(Long recipientId, NotificationType type, Long referenceId) {
        return new Notification(recipientId, type, referenceId);
    }
}
