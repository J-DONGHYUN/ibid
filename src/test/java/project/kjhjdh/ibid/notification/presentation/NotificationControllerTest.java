package project.kjhjdh.ibid.notification.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.notification.domain.Notification;
import project.kjhjdh.ibid.notification.domain.NotificationType;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class NotificationControllerTest extends ControllerTestSupport {

    @DisplayName("[NT-03] 알림함을 조회하면 200과 내 알림 목록·다음 커서를 응답한다")
    @Test
    void list() {
        // given
        Notification n = Notification.create(1L, NotificationType.NEW_MESSAGE, 5L);
        ReflectionTestUtils.setField(n, "id", 9L);
        given(notificationService.getNotifications(anyLong(), any()))
                .willReturn(new SliceImpl<>(List.of(n), PageRequest.of(0, 20), true));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/notifications")
                .then()
                .statusCode(200)
                .body("notifications[0].notificationId", equalTo(9))
                .body("notifications[0].type", equalTo("NEW_MESSAGE"))
                .body("notifications[0].read", equalTo(false))
                .body("nextCursor", equalTo(9))
                .body("hasNext", equalTo(true));
    }

    @DisplayName("[NT-03] 알림을 읽음 처리하면 200을 응답하고 서비스에 본인과 알림을 넘긴다")
    @Test
    void read() {
        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/notifications/{id}/read", 9L)
                .then()
                .statusCode(200);

        then(notificationService).should().markRead(eq(9L), anyLong());
    }

    @DisplayName("[NT-03] 남의 알림을 읽음 처리하면 403을 응답한다")
    @Test
    void read_notOwner() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(notificationService).markRead(anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/notifications/{id}/read", 9L)
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @DisplayName("[NT-03] 없는 알림을 읽음 처리하면 404를 응답한다")
    @Test
    void read_notFound() {
        // given
        willThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND))
                .given(notificationService).markRead(anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/notifications/{id}/read", 9L)
                .then()
                .statusCode(404)
                .body("code", equalTo("NOTIFICATION_NOT_FOUND"));
    }
}
