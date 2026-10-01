package project.kjhjdh.ibid.trade.application;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.product.application.ProductImageService;
import project.kjhjdh.ibid.product.application.ProductListResult;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Service
@RequiredArgsConstructor
public class TradeHistoryService {

    private static final int PAGE_SIZE = 16;

    private final ProductRepository productRepository;
    private final ProductImageService productImageService;

    @Transactional(readOnly = true)
    public ProductListResult getSales(Long sellerId, ProductStatus status, Long cursor) {
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Slice<Product> slice = (status == null)
                ? productRepository.findBySellerIdAndIdLessThanOrderByIdDesc(sellerId, effectiveCursor, pageable)
                : productRepository.findBySellerIdAndStatusAndIdLessThanOrderByIdDesc(sellerId, status, effectiveCursor, pageable);
        return toResult(slice);
    }

    @Transactional(readOnly = true)
    public ProductListResult getPurchases(Long buyerId, Long cursor) {
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        Slice<Product> slice = productRepository.findBySoldBuyerIdAndIdLessThanOrderByIdDesc(
                buyerId, effectiveCursor, PageRequest.of(0, PAGE_SIZE));
        return toResult(slice);
    }

    private ProductListResult toResult(Slice<Product> slice) {
        List<Long> productIds = slice.getContent().stream().map(Product::getId).toList();
        return new ProductListResult(slice, productImageService.findThumbnails(productIds));
    }
}
