package project.kjhjdh.ibid.chat.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.chat.application.SendMessageResult;
import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.chat.presentation.dto.ChatMessageResponse;
import project.kjhjdh.ibid.chat.presentation.dto.SendMessageRequest;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class ChatMessageControllerTest extends ControllerTestSupport {

    @DisplayName("[CH-02] 메시지 전송에 성공하면 201과 메시지 정보를 응답한다")
    @Test
    void send() {
        // given
        ChatMessage message = ChatMessage.create(1L, 1L, "안녕하세요", "c-1");
        ReflectionTestUtils.setField(message, "id", 5L);
        given(chatMessageService.send(any())).willReturn(new SendMessageResult(message, true));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new SendMessageRequest("안녕하세요", "c-1"))
                .when()
                .post("/api/chat-rooms/{chatRoomId}/messages", 1L)
                .then()
                .statusCode(201)
                .body("messageId", equalTo(5))
                .body("content", equalTo("안녕하세요"));
    }

    @DisplayName("[CH-02] 새로 저장된 메시지는 그 방 토픽 구독자에게 브로드캐스트된다")
    @Test
    void send_broadcastsToRoomTopic() {
        // given
        ChatMessage message = ChatMessage.create(1L, 1L, "안녕하세요", "c-1");
        ReflectionTestUtils.setField(message, "id", 5L);
        given(chatMessageService.send(any())).willReturn(new SendMessageResult(message, true));

        // when
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new SendMessageRequest("안녕하세요", "c-1"))
                .when()
                .post("/api/chat-rooms/{chatRoomId}/messages", 1L)
                .then()
                .statusCode(201);

        // then
        then(messagingTemplate).should().convertAndSend(eq("/topic/room.1"), any(ChatMessageResponse.class));
    }

    @DisplayName("[CH-02] 재전송(멱등)한 메시지는 다시 브로드캐스트하지 않는다")
    @Test
    void send_doesNotRebroadcastOnResend() {
        // given
        ChatMessage existing = ChatMessage.create(1L, 1L, "안녕하세요", "c-1");
        ReflectionTestUtils.setField(existing, "id", 5L);
        given(chatMessageService.send(any())).willReturn(new SendMessageResult(existing, false));

        // when
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new SendMessageRequest("안녕하세요", "c-1"))
                .when()
                .post("/api/chat-rooms/{chatRoomId}/messages", 1L)
                .then()
                .statusCode(201);

        // then
        then(messagingTemplate).should(never()).convertAndSend(any(String.class), any(Object.class));
    }

    @DisplayName("[CH-02] 빈 메시지를 보내면 400을 응답한다")
    @Test
    void send_blankContent() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new SendMessageRequest("", "c-1"))
                .when()
                .post("/api/chat-rooms/{chatRoomId}/messages", 1L)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("[I-08] 참여자가 아니면 403을 응답한다")
    @Test
    void send_notParticipant() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(chatMessageService).send(any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new SendMessageRequest("안녕하세요", "c-1"))
                .when()
                .post("/api/chat-rooms/{chatRoomId}/messages", 1L)
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }
}
