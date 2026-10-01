package project.kjhjdh.ibid.chat.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.chat.application.MyChatRoomResult;
import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.product.application.ProductSummary;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class ChatRoomQueryControllerTest extends ControllerTestSupport {

    @DisplayName("[CH-05] 내 채팅방 목록을 조회하면 방마다 상대·상품·마지막 메시지·안 읽은 수와 다음 커서를 응답한다")
    @Test
    void myRooms() {
        // given
        ChatMessage lastMessage = ChatMessage.create(5L, 10L, "마지막", "c-3");
        ReflectionTestUtils.setField(lastMessage, "id", 3L);
        MyChatRoomResult result = new MyChatRoomResult(
                5L, 10L, new ProductSummary(1L, "아이폰 13", "https://thumb"), lastMessage, 4L);
        given(chatRoomService.getMyRooms(any(), any()))
                .willReturn(new SliceImpl<>(List.of(result), PageRequest.of(0, 20), true));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/chat-rooms")
                .then()
                .statusCode(200)
                .body("rooms[0].chatRoomId", equalTo(5))
                .body("rooms[0].peerId", equalTo(10))
                .body("rooms[0].product.title", equalTo("아이폰 13"))
                .body("rooms[0].lastMessage.messageId", equalTo(3))
                .body("rooms[0].unreadCount", equalTo(4))
                .body("nextCursor", equalTo(3))
                .body("hasNext", equalTo(true));
    }

    @DisplayName("[CH-05] 채팅방이 없으면 빈 목록과 다음 커서 없음을 응답한다")
    @Test
    void myRooms_empty() {
        // given
        given(chatRoomService.getMyRooms(any(), any()))
                .willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 20), false));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/chat-rooms")
                .then()
                .statusCode(200)
                .body("rooms.size()", equalTo(0))
                .body("nextCursor", equalTo(null))
                .body("hasNext", equalTo(false));
    }
}
