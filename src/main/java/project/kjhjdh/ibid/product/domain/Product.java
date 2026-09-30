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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Product(Long sellerId, String title, String description, int price,
                    ProductCondition productCondition) {
        validateTitle(title);
        validateDescription(description);
        validatePrice(price);
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.status = ProductStatus.ON_SALE;
        this.productCondition = productCondition;
        this.createdAt = LocalDateTime.now();
    }

    public static Product create(Long sellerId, String title, String description, int price) {
        return create(sellerId, title, description, price, ProductCondition.USED);
    }

    public static Product create(Long sellerId, String title, String description, int price, ProductCondition productCondition) {
        return new Product(sellerId, title, description, price, productCondition);
    }

    public void update(String title, String description, int price,
                       ProductCondition productCondition) {
        validateTitle(title);
        validateDescription(description);
        validatePrice(price);
        this.title = title;
        this.description = description;
        this.price = price;
        this.productCondition = productCondition;
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
}
