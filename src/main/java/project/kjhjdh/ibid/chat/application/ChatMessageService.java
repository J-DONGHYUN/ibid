package project.kjhjdh.ibid.chat.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.chat.domain.ChatMessage;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatMessageRepository;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int PAGE_SIZE = 30;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public MessageListResult getMessages(Long chatRoomId, Long userId, Long cursor) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        Slice<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(
                chatRoomId, effectiveCursor, PageRequest.of(0, PAGE_SIZE));
        return new MessageListResult(messages,
                room.getSellerLastReadMessageId(), room.getBuyerLastReadMessageId());
    }

    @Transactional
    public ReadReceiptResult markRead(Long chatRoomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        chatMessageRepository.findFirstByChatRoomIdOrderByIdDesc(chatRoomId)
                .ifPresent(latest -> {
                    room.markRead(userId, latest.getId());
                    chatRoomRepository.save(room);
                });
        return new ReadReceiptResult(chatRoomId, userId, room.lastReadMessageIdOf(userId));
    }

    public SendMessageResult send(SendMessageCommand command) {
        ChatRoom room = chatRoomRepository.findById(command.chatRoomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.isParticipant(command.senderId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        boolean productDeleted = productRepository.findById(room.getProductId())
                .map(Product::isDeleted)
                .orElse(false);
        if (productDeleted) {
            throw new BusinessException(ErrorCode.PRODUCT_DELETED);
        }
        return chatMessageRepository.findByChatRoomIdAndClientMessageId(command.chatRoomId(), command.clientMessageId())
                .map(existing -> new SendMessageResult(existing, false))
                .orElseGet(() -> save(command));
    }

    private SendMessageResult save(SendMessageCommand command) {
        try {
            ChatMessage saved = chatMessageRepository.save(ChatMessage.create(
                    command.chatRoomId(), command.senderId(), command.content(), command.clientMessageId()));
            return new SendMessageResult(saved, true);
        } catch (DataIntegrityViolationException e) {
            ChatMessage existing = chatMessageRepository.findByChatRoomIdAndClientMessageId(
                    command.chatRoomId(), command.clientMessageId()).orElseThrow(() -> e);
            return new SendMessageResult(existing, false);
        }
    }
}
