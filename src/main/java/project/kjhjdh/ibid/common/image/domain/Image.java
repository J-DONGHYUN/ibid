package project.kjhjdh.ibid.common.image.domain;

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

@Getter
@Entity
@Table(name = "images", indexes = @Index(name = "idx_images_owner", columnList = "ownerType, ownerId, sortOrder"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageOwnerType ownerType;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageExtension extension;

    @Column(nullable = false)
    private int sortOrder;

    private Image(ImageOwnerType ownerType, Long ownerId, String url, int sortOrder) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.url = url;
        this.extension = ImageExtension.fromUrl(url);
        this.sortOrder = sortOrder;
    }

    public static Image of(ImageOwnerType ownerType, Long ownerId, String url, int sortOrder) {
        return new Image(ownerType, ownerId, url, sortOrder);
    }
}
