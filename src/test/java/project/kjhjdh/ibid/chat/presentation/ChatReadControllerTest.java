package project.kjhjdh.ibid.chat.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.chat.application.ReadReceiptResult;
import project.kjhjdh.ibid.chat.presentation.dto.ReadReceiptResponse;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class ChatReadControllerTest extends ControllerTestSupport {

    @DisplayName("[CH-06] 읽음 처리하면 200과 읽음 위치를 응답하고 그 방 토픽으로 읽음 이벤트를 보낸다")
    @Test
    void read() {
        // given
        given(chatMessageService.markRead(any(), any()))
                .willReturn(new ReadReceiptResult(1L, 1L, 9L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/chat-rooms/{chatRoomId}/read", 1L)
                .then()
                .statusCode(200)
                .body("chatRoomId", equalTo(1))
                .body("readerId", equalTo(1))
                .body("lastReadMessageId", equalTo(9));

        then(messagingTemplate).should().convertAndSend(eq("/topic/room.1"), any(ReadReceiptResponse.class));
    }

    @DisplayName("[I-08] 참여자가 아니면 읽음 처리에 403을 응답한다")
    @Test
    void read_notParticipant() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(chatMessageService).markRead(any(), any());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/chat-rooms/{chatRoomId}/read", 1L)
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @DisplayName("[CH-06] 없는 채팅방은 읽음 처리에 404를 응답한다")
    @Test
    void read_roomNotFound() {
        // given
        willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                .given(chatMessageService).markRead(any(), any());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/chat-rooms/{chatRoomId}/read", 1L)
                .then()
                .statusCode(404)
                .body("code", equalTo("CHAT_ROOM_NOT_FOUND"));
    }
}
