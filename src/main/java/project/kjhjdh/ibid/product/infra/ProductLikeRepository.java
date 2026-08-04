package project.kjhjdh.ibid.product.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.product.domain.ProductLike;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    long countByProductId(Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    List<ProductLike> findByUserIdOrderByIdDesc(Long userId);
}