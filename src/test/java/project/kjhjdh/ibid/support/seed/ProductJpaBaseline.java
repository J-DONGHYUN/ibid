package project.kjhjdh.ibid.support.seed;

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.support.TransactionTemplate;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;

public class ProductJpaBaseline {

    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;

    public ProductJpaBaseline(ProductRepository productRepository, TransactionTemplate transactionTemplate) {
        this.productRepository = productRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public long insert(int totalRows, int batchSize) {
        long startedAt = System.nanoTime();
        for (int offset = 0; offset < totalRows; offset += batchSize) {
            int size = Math.min(batchSize, totalRows - offset);
            List<Product> batch = buildBatch(offset, size);
            transactionTemplate.executeWithoutResult(status -> productRepository.saveAll(batch));
        }
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private List<Product> buildBatch(int offset, int size) {
        List<Product> batch = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int index = offset + i;
            batch.add(Product.create(
                    1L,
                    "JPA 기준선 상품 #" + index,
                    "saveAll 처리량 측정용 데이터입니다.",
                    10_000,
                    1,
                    ProductCondition.USED,
                    List.of(),
                    0
            ));
        }
        return batch;
    }
}
