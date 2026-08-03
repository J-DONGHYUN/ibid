package project.kjhjdh.ibid.common.image.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.common.image.domain.Image;
import project.kjhjdh.ibid.common.image.domain.ImageOwnerType;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByOwnerTypeAndOwnerIdOrderBySortOrder(ImageOwnerType ownerType, Long ownerId);

    List<Image> findByOwnerTypeAndOwnerIdInOrderBySortOrder(ImageOwnerType ownerType, List<Long> ownerIds);

    List<Image> findByOwnerTypeAndOwnerIdAndUrlIn(ImageOwnerType ownerType, Long ownerId, List<String> urls);

    List<Image> findByOwnerTypeAndOwnerId(ImageOwnerType ownerType, Long ownerId);

    int countByOwnerTypeAndOwnerId(ImageOwnerType ownerType, Long ownerId);
}
