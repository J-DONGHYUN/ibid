package project.kjhjdh.ibid.chat.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatMessageRepository;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatMessage send(SendMessageCommand command) {
        ChatRoom room = chatRoomRepository.findById(command.chatRoomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.isParticipant(command.senderId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return chatMessageRepository.findByChatRoomIdAndClientMessageId(command.chatRoomId(), command.clientMessageId())
                .orElseGet(() -> save(command));
    }

    private ChatMessage save(SendMessageCommand command) {
        try {
            return chatMessageRepository.save(ChatMessage.create(
                    command.chatRoomId(), command.senderId(), command.content(), command.clientMessageId()));
        } catch (DataIntegrityViolationException e) {
            return chatMessageRepository.findByChatRoomIdAndClientMessageId(command.chatRoomId(), command.clientMessageId())
                    .orElseThrow(() -> e);
        }
    }
}
