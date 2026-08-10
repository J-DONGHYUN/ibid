package project.kjhjdh.ibid.product.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.product.domain.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdInOrderBySortOrder(List<Long> productIds);
}
