package project.kjhjdh.ibid.trade.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.chat.application.ChatRoomService;
import project.kjhjdh.ibid.common.event.NotificationMessage;
import project.kjhjdh.ibid.common.event.NotificationMessage.NotificationType;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ProductRepository productRepository;
    private final ChatRoomService chatRoomService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void reserve(Long productId, Long sellerId, Long buyerId) {
        Product product = findOwnedProduct(productId, sellerId);
        if (!chatRoomService.isChatPartner(productId, buyerId)) {
            throw new BusinessException(ErrorCode.NOT_CHAT_PARTNER);
        }
        product.reserve(buyerId);
        eventPublisher.publishEvent(new NotificationMessage(buyerId, NotificationType.RESERVED, productId));
    }

    @Transactional
    public void cancelReservation(Long productId, Long sellerId) {
        Product product = findOwnedProduct(productId, sellerId);
        Long reservedBuyerId = product.getReservedBuyerId();
        product.cancelReservation();
        if (reservedBuyerId != null) {
            eventPublisher.publishEvent(
                    new NotificationMessage(reservedBuyerId, NotificationType.RESERVATION_CANCELED, productId));
        }
    }

    private Product findOwnedProduct(Long productId, Long sellerId) {
        Product product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isOwnedBy(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return product;
    }
}
