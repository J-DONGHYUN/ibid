package project.kjhjdh.ibid.product.infra;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import project.kjhjdh.ibid.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Slice<Product> findByIdLessThanOrderByIdDesc(Long id, Pageable pageable);

    List<Product> findBySellerIdOrderByIdDesc(Long sellerId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.viewCount = p.viewCount + :delta where p.id = :id")
    int increaseViewCount(@Param("id") Long id, @Param("delta") long delta);
}
