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
                apply(productId, delta);
            }
        }
    }

    private void apply(Long productId, long delta) {
        try {
            productRepository.increaseViewCount(productId, delta);
        } catch (RuntimeException e) {
            productViewCounter.restorePendingCount(productId, delta);
            throw e;
        }
    }
}
