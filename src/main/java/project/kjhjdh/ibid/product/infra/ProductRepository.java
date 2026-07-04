package project.kjhjdh.ibid.product.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByIdDesc();
}
