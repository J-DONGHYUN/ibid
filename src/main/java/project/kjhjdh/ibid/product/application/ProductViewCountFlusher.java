package project.kjhjdh.ibid.product.application;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Component
@RequiredArgsConstructor
public class ProductViewCountFlusher {

    private final ProductViewCounter productViewCounter;
    private final ProductRepository productRepository;

    public void flush() {
        for (Long productId : productViewCounter.readPendingProductIds()) {
            long delta = productViewCounter.takePendingCount(productId);
            if (delta > 0) {
                productRepository.increaseViewCount(productId, delta);
            }
        }
    }
}
