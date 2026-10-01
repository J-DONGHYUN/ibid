package project.kjhjdh.ibid.chat.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import project.kjhjdh.ibid.chat.infra.RoomLastMessage;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.application.ProductService;
import project.kjhjdh.ibid.product.application.ProductSummary;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final int PAGE_SIZE = 20;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public ChatRoom open(Long productId, Long buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId)
                .orElseGet(() -> save(product, buyerId));
    }

    @Transactional(readOnly = true)
    public boolean isParticipant(Long chatRoomId, Long userId) {
        return chatRoomRepository.findById(chatRoomId)
                .map(room -> room.isParticipant(userId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Slice<MyChatRoomResult> getMyRooms(Long userId, Long cursor) {
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        Slice<RoomLastMessage> slice = chatRoomRepository.findMyRoomsOrderByLastMessageDesc(
                userId, effectiveCursor, PageRequest.of(0, PAGE_SIZE));

        Map<Long, ChatRoom> rooms = chatRoomRepository.findAllById(
                        slice.getContent().stream().map(RoomLastMessage::getRoomId).toList()).stream()
                .collect(Collectors.toMap(ChatRoom::getId, Function.identity()));
        Map<Long, ChatMessage> lastMessages = chatMessageRepository.findAllById(
                        slice.getContent().stream().map(RoomLastMessage::getLastMessageId).toList()).stream()
                .collect(Collectors.toMap(ChatMessage::getId, Function.identity()));
        Map<Long, ProductSummary> summaries = productService.findSummaries(
                rooms.values().stream().map(ChatRoom::getProductId).toList());

        return slice.map(entry -> toResult(userId, rooms.get(entry.getRoomId()),
                lastMessages.get(entry.getLastMessageId()), summaries));
    }

    @Transactional(readOnly = true)
    public Slice<ProductChatRoomResult> getProductRooms(Long productId, Long sellerId, Long cursor) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isOwnedBy(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        Slice<ChatRoom> slice = chatRoomRepository.findByProductIdAndSellerIdAndIdLessThanOrderByIdDesc(
                productId, sellerId, effectiveCursor, PageRequest.of(0, PAGE_SIZE));
        return slice.map(room -> toProductRoomResult(room, sellerId));
    }

    private ProductChatRoomResult toProductRoomResult(ChatRoom room, Long sellerId) {
        ChatMessage lastMessage = chatMessageRepository.findFirstByChatRoomIdOrderByIdDesc(room.getId())
                .orElse(null);
        Long lastRead = room.lastReadMessageIdOf(sellerId);
        long unreadCount = chatMessageRepository.countByChatRoomIdAndIdGreaterThanAndSenderIdNot(
                room.getId(), lastRead == null ? 0L : lastRead, sellerId);
        return new ProductChatRoomResult(room.getId(), room.getBuyerId(), lastMessage, unreadCount);
    }

    private MyChatRoomResult toResult(Long userId, ChatRoom room, ChatMessage lastMessage,
                                      Map<Long, ProductSummary> summaries) {
        Long peerId = room.getSellerId().equals(userId) ? room.getBuyerId() : room.getSellerId();
        Long lastRead = room.lastReadMessageIdOf(userId);
        long unreadCount = chatMessageRepository.countByChatRoomIdAndIdGreaterThanAndSenderIdNot(
                room.getId(), lastRead == null ? 0L : lastRead, userId);
        return new MyChatRoomResult(room.getId(), peerId,
                summaries.get(room.getProductId()), lastMessage, unreadCount);
    }

    private ChatRoom save(Product product, Long buyerId) {
        try {
            return chatRoomRepository.save(ChatRoom.open(product.getId(), product.getSellerId(), buyerId));
        } catch (DataIntegrityViolationException e) {
            return chatRoomRepository.findByProductIdAndBuyerId(product.getId(), buyerId)
                    .orElseThrow(() -> e);
        }
    }
}
