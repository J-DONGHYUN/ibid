package project.kjhjdh.ibid.trade.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.chat.application.ChatRoomService;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Service
@RequiredArgsConstructor
public class CompletionService {

    private final ProductRepository productRepository;
    private final ChatRoomService chatRoomService;

    @Transactional
    public void complete(Long productId, Long sellerId, Long buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isOwnedBy(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!chatRoomService.isChatPartner(productId, buyerId)) {
            throw new BusinessException(ErrorCode.NOT_CHAT_PARTNER);
        }
        product.complete(buyerId);
    }
}
