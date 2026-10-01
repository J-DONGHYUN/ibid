package project.kjhjdh.ibid.chat.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.chat.application.ChatMessageService;
import project.kjhjdh.ibid.chat.application.SendMessageCommand;
import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.chat.presentation.dto.ChatMessageResponse;
import project.kjhjdh.ibid.chat.presentation.dto.SendMessageRequest;

@RestController
@RequestMapping("/api/chat-rooms/{chatRoomId}/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> send(
            @LoginUser UserInfo loginUser,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        ChatMessage message = chatMessageService.send(new SendMessageCommand(
                chatRoomId, loginUser.userId(), request.clientMessageId(), request.content()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ChatMessageResponse.from(message));
    }
}
