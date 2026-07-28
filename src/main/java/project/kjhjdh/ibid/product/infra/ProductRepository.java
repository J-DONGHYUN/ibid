package project.kjhjdh.ibid.product.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import project.kjhjdh.ibid.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Slice<Product> findByIdLessThanOrderByIdDesc(Long id, Pageable pageable);

    List<Product> findBySellerIdOrderByIdDesc(Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
