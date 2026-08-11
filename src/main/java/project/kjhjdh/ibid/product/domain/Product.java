package project.kjhjdh.ibid.product.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
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

    @Column(nullable = false)
    private long viewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCondition productCondition;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<ProductImage> images = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "product_tag",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    @Column(nullable = false)
    private int shippingFee;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Product(Long sellerId, String title, String description, int price, int stock,
                    ProductCondition productCondition, List<Tag> tags, int shippingFee) {
        validateTitle(title);
        validateDescription(description);
        validatePrice(price);
        validateStock(stock);
        validateShippingFee(shippingFee);
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.status = ProductStatus.PENDING;
        this.productCondition = productCondition;
        this.tags = new ArrayList<>(tags);
        this.shippingFee = shippingFee;
        this.createdAt = LocalDateTime.now();
    }

    public static Product create(Long sellerId, String title, String description, int price, int stock) {
        return create(sellerId, title, description, price, stock, ProductCondition.USED);
    }

    public static Product create(Long sellerId, String title, String description, int price, int stock, ProductCondition productCondition) {
        return create(sellerId, title, description, price, stock, productCondition, List.of(), 0);
    }

    public static Product create(Long sellerId, String title, String description, int price, int stock,
                                 ProductCondition productCondition, List<Tag> tags, int shippingFee) {
        return new Product(sellerId, title, description, price, stock, productCondition, tags, shippingFee);
    }

    public void openForSale() {
        if (status != ProductStatus.PENDING) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_PENDING);
        }
        this.status = ProductStatus.ON_SALE;
    }

    public void decreaseStock(int quantity) {
        validateAvailableStock(quantity);
        this.stock -= quantity;
        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }

    public void validatePurchasable(Long buyerId, int quantity) {
        if (isOwnedBy(buyerId)) {
            throw new BusinessException(ErrorCode.SELF_TRADE_NOT_ALLOWED);
        }
        validateAvailableStock(quantity);
    }

    private void validateAvailableStock(int quantity) {
        if (quantity < MIN_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_PURCHASE_QUANTITY);
        }
        if (isSoldOut()) {
            throw new BusinessException(ErrorCode.SOLD_OUT);
        }
        if (status != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
        if (quantity > stock) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    public void restoreStock(int quantity) {
        if (quantity < MIN_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_PURCHASE_QUANTITY);
        }
        if (status == ProductStatus.PENDING) {
            throw new BusinessException(ErrorCode.CANNOT_RESTORE_STOCK);
        }
        this.stock += quantity;
        if (status == ProductStatus.SOLD_OUT) {
            this.status = ProductStatus.ON_SALE;
        }
    }

    public boolean isSoldOut() {
        return status == ProductStatus.SOLD_OUT;
    }

    public boolean isOnSale() {
        return status == ProductStatus.ON_SALE;
    }

    public void update(String title, String description, int price, int stock,
                       ProductCondition productCondition, List<Tag> tags, int shippingFee) {
        validateModifiable();
        validateTitle(title);
        validateDescription(description);
        validatePrice(price);
        validateStock(stock);
        validateShippingFee(shippingFee);
        this.title = title;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.productCondition = productCondition;
        this.tags.clear();
        this.tags.addAll(tags);
        this.shippingFee = shippingFee;
    }

    public void validateModifiable() {
        if (status == ProductStatus.SOLD_OUT) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_MODIFIABLE);
        }
    }

    public void addImages(List<String> urls) {
        int base = images.size();
        for (int i = 0; i < urls.size(); i++) {
            images.add(ProductImage.of(this, urls.get(i), base + i));
        }
    }

    public List<String> removeImages(List<String> urls) {
        List<ProductImage> targets = images.stream()
                .filter(image -> urls.contains(image.getUrl()))
                .toList();
        images.removeAll(targets);
        return targets.stream().map(ProductImage::getUrl).toList();
    }

    public List<String> imageUrls() {
        return images.stream().map(ProductImage::getUrl).toList();
    }

    public List<String> tagNames() {
        return tags.stream().map(Tag::getName).toList();
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

    private void validateShippingFee(int shippingFee) {
        if (shippingFee < 0) {
            throw new BusinessException(ErrorCode.INVALID_SHIPPING_FEE);
        }
    }
}
