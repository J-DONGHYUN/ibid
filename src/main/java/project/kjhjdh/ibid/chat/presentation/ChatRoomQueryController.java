package project.kjhjdh.ibid.chat.presentation;

import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.chat.application.ChatRoomService;
import project.kjhjdh.ibid.chat.application.MyChatRoomResult;
import project.kjhjdh.ibid.chat.presentation.dto.MyChatRoomListResponse;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomQueryController {

    private final ChatRoomService chatRoomService;

    @GetMapping
    public ResponseEntity<MyChatRoomListResponse> myRooms(
            @LoginUser UserInfo loginUser,
            @RequestParam(required = false) Long cursor
    ) {
        Slice<MyChatRoomResult> slice = chatRoomService.getMyRooms(loginUser.userId(), cursor);
        return ResponseEntity.ok(MyChatRoomListResponse.of(slice));
    }
}
