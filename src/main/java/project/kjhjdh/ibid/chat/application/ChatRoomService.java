package project.kjhjdh.ibid.chat.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ProductRepository productRepository;

    public ChatRoom open(Long productId, Long buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId)
                .orElseGet(() -> save(product, buyerId));
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
