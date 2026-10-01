package project.kjhjdh.ibid.chat.presentation;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.chat.application.ChatMessageService;
import project.kjhjdh.ibid.chat.application.ReadReceiptResult;
import project.kjhjdh.ibid.chat.presentation.dto.ReadReceiptResponse;

@RestController
@RequestMapping("/api/chat-rooms/{chatRoomId}/read")
@RequiredArgsConstructor
public class ChatReadController {

    private static final String TOPIC_ROOM_PREFIX = "/topic/room.";

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<ReadReceiptResponse> read(
            @LoginUser UserInfo loginUser,
            @PathVariable Long chatRoomId
    ) {
        ReadReceiptResult result = chatMessageService.markRead(chatRoomId, loginUser.userId());
        ReadReceiptResponse response = ReadReceiptResponse.from(result);
        messagingTemplate.convertAndSend(TOPIC_ROOM_PREFIX + chatRoomId, response);
        return ResponseEntity.ok(response);
    }
}
