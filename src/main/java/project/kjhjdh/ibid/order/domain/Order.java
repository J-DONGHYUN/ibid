package project.kjhjdh.ibid.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    private static final int MIN_QUANTITY = 1;
    private static final int MIN_UNIT_PRICE = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private Order(Long productId, Long buyerId, Long sellerId, int quantity, int unitPrice) {
        validateQuantity(quantity);
        validateUnitPrice(unitPrice);
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.quantity = quantity;
        this.totalPrice = unitPrice * quantity;
        this.status = OrderStatus.CREATED;
    }

    public static Order create(Long productId, Long buyerId, Long sellerId, int quantity, int unitPrice) {
        return new Order(productId, buyerId, sellerId, quantity, unitPrice);
    }

    public void confirmPaid() {
        if (status != OrderStatus.CREATED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE);
        }
        this.status = OrderStatus.PAID;
    }

    public void ship() {
        if (status != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_NOT_SHIPPABLE);
        }
        this.status = OrderStatus.SHIPPED_TO_INSPECTOR;
    }

    public void startInspection() {
        if (status != OrderStatus.SHIPPED_TO_INSPECTOR) {
            throw new BusinessException(ErrorCode.ORDER_NOT_INSPECTABLE);
        }
        this.status = OrderStatus.UNDER_INSPECTION;
    }

    public void complete() {
        if (status != OrderStatus.UNDER_INSPECTION) {
            throw new BusinessException(ErrorCode.ORDER_NOT_JUDGEABLE);
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void refund() {
        if (status != OrderStatus.UNDER_INSPECTION) {
            throw new BusinessException(ErrorCode.ORDER_NOT_JUDGEABLE);
        }
        this.status = OrderStatus.REFUNDED;
    }

    public boolean isSeller(Long userId) {
        return sellerId.equals(userId);
    }

    public boolean isBuyer(Long userId) {
        return buyerId.equals(userId);
    }

    public void cancel() {
        if (status != OrderStatus.CREATED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE);
        }
        this.status = OrderStatus.CANCELED;
    }

    private void validateQuantity(int quantity) {
        if (quantity < MIN_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_PURCHASE_QUANTITY);
        }
    }

    private void validateUnitPrice(int unitPrice) {
        if (unitPrice < MIN_UNIT_PRICE) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_PRICE);
        }
    }
}
