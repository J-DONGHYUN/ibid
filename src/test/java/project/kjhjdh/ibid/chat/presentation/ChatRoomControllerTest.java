package project.kjhjdh.ibid.chat.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.chat.application.ProductChatRoomResult;
import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class ChatRoomControllerTest extends ControllerTestSupport {

    @DisplayName("[CH-01] 채팅방 열기에 성공하면 201과 채팅방 정보를 응답한다")
    @Test
    void open() {
        // given
        ChatRoom room = ChatRoom.open(1L, 5L, 1L);
        ReflectionTestUtils.setField(room, "id", 7L);
        given(chatRoomService.open(eq(1L), anyLong())).willReturn(room);

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/products/{productId}/chat-rooms", 1L)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("chatRoomId", equalTo(7))
                .body("productId", equalTo(1))
                .body("buyerId", equalTo(1));
    }

    @DisplayName("[I-05] 본인 상품에 채팅방을 열면 400을 응답한다")
    @Test
    void open_ownProduct() {
        // given
        willThrow(new BusinessException(ErrorCode.CANNOT_OPEN_CHAT_ON_OWN_PRODUCT))
                .given(chatRoomService).open(eq(1L), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/products/{productId}/chat-rooms", 1L)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("CANNOT_OPEN_CHAT_ON_OWN_PRODUCT"));
    }

    @DisplayName("[CH-01] 존재하지 않는 상품에 채팅방을 열면 404를 응답한다")
    @Test
    void open_productNotFound() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .given(chatRoomService).open(eq(999L), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/products/{productId}/chat-rooms", 999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("PRODUCT_NOT_FOUND"));
    }

    @DisplayName("[CH-07] 상품별 채팅방 목록을 조회하면 방마다 상대·마지막 메시지·안 읽은 수와 다음 커서를 응답한다")
    @Test
    void productRooms() {
        // given
        ChatMessage lastMessage = ChatMessage.create(5L, 20L, "안녕하세요", "c-1");
        ReflectionTestUtils.setField(lastMessage, "id", 3L);
        ProductChatRoomResult result = new ProductChatRoomResult(5L, 20L, lastMessage, 2L);
        given(chatRoomService.getProductRooms(eq(1L), anyLong(), any()))
                .willReturn(new SliceImpl<>(List.of(result), PageRequest.of(0, 20), true));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/{productId}/chat-rooms", 1L)
                .then()
                .statusCode(200)
                .body("rooms[0].chatRoomId", equalTo(5))
                .body("rooms[0].buyerId", equalTo(20))
                .body("rooms[0].lastMessage.messageId", equalTo(3))
                .body("rooms[0].unreadCount", equalTo(2))
                .body("nextCursor", equalTo(5))
                .body("hasNext", equalTo(true));
    }

    @DisplayName("[CH-07] 판매자 본인이 아니면 상품별 채팅방 조회에 403을 응답한다")
    @Test
    void productRooms_notOwner() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(chatRoomService).getProductRooms(eq(1L), anyLong(), any());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/{productId}/chat-rooms", 1L)
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }
}
