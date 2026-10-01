package project.kjhjdh.ibid.chat.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.chat.application.ChatRoomService;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.presentation.dto.ChatRoomResponse;

@RestController
@RequestMapping("/api/products/{productId}/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<ChatRoomResponse> open(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId
    ) {
        ChatRoom chatRoom = chatRoomService.open(productId, loginUser.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ChatRoomResponse.from(chatRoom));
    }
}
