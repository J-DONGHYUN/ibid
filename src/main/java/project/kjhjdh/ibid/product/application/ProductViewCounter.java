package project.kjhjdh.ibid.product.application;

import java.time.Duration;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductViewRedisRepository;

@Component
public class ProductViewCounter {

    private final ProductViewRedisRepository productViewRedisRepository;
    private final Duration dedupTtl;

    public ProductViewCounter(ProductViewRedisRepository productViewRedisRepository,
                              @Value("${product.view.dedup-ttl}") Duration dedupTtl) {
        this.productViewRedisRepository = productViewRedisRepository;
        this.dedupTtl = dedupTtl;
    }

    public void record(Long productId, String visitorId) {
        productViewRedisRepository.recordView(productId, visitorId, dedupTtl);
    }

    public long readTotal(Product product) {
        return product.getViewCount() + productViewRedisRepository.findPendingCount(product.getId());
    }

    public Set<Long> readPendingProductIds() {
        return productViewRedisRepository.findPendingProductIds();
    }

    public long takePendingCount(Long productId) {
        return productViewRedisRepository.takePendingCount(productId);
    }
}
