package project.kjhjdh.ibid.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.kjhjdh.ibid.common.image.domain.ImageExtension;

@Getter
@Entity
@Table(name = "product_images", indexes = @Index(name = "idx_product_images_product", columnList = "productId, sortOrder"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageExtension extension;

    @Column(nullable = false)
    private int sortOrder;

    private ProductImage(Long productId, String url, int sortOrder) {
        this.productId = productId;
        this.url = url;
        this.extension = ImageExtension.fromUrl(url);
        this.sortOrder = sortOrder;
    }

    public static ProductImage of(Long productId, String url, int sortOrder) {
        return new ProductImage(productId, url, sortOrder);
    }
}
