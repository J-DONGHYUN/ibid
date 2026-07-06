package project.kjhjdh.ibid.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    private static final int TITLE_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int MIN_PRICE = 1;
    private static final int MIN_STOCK = 1;
    private static final int MIN_QUANTITY = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Version
    private Long version;

    private Product(Long sellerId, String title, String description, int price, int stock) {
        validateTitle(title);
        validateDescription(description);
        validatePrice(price);
        validateStock(stock);
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.status = ProductStatus.ON_SALE;
    }

    public static Product create(Long sellerId, String title, String description, int price, int stock) {
        return new Product(sellerId, title, description, price, stock);
    }

    public void decreaseStock(int quantity) {
        if (quantity < MIN_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_PURCHASE_QUANTITY);
        }
        if (isSoldOut()) {
            throw new BusinessException(ErrorCode.SOLD_OUT);
        }
        if (quantity > stock) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;
        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }

    public boolean isSoldOut() {
        return status == ProductStatus.SOLD_OUT;
    }

    public boolean isOwnedBy(Long userId) {
        return sellerId.equals(userId);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > TITLE_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_TITLE);
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank() || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_DESCRIPTION);
        }
    }

    private void validatePrice(int price) {
        if (price < MIN_PRICE) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_PRICE);
        }
    }

    private void validateStock(int stock) {
        if (stock < MIN_STOCK) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_STOCK);
        }
    }
}
