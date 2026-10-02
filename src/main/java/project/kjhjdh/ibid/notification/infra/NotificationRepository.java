package project.kjhjdh.ibid.notification.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
