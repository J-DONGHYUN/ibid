package project.kjhjdh.ibid.product.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.common.image.infra.S3ImageUploader;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductImage;
import project.kjhjdh.ibid.product.infra.ProductImageRepository;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignResponse;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final String DIRECTORY_PREFIX = "products/";

    private final ProductImageRepository productImageRepository;
    private final S3ImageUploader s3ImageUploader;

    public List<ImagePresignResponse> presign(Long productId, List<ImagePresignRequest> requests) {
        String directory = DIRECTORY_PREFIX + productId;
        return requests.stream()
                .map(req -> {
                    PresignedUploadResult result =
                            s3ImageUploader.generatePresignedUrl(directory, req.filename(), req.contentType());
                    return new ImagePresignResponse(result.presignedUrl(), result.key(), result.imageUrl());
                })
                .toList();
    }

    @Transactional
    public void attach(Product product, List<String> urls) {
        int base = productImageRepository.countByProductId(product.getId());
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            images.add(ProductImage.of(product, urls.get(i), base + i));
        }
        productImageRepository.saveAll(images);
    }

    @Transactional(readOnly = true)
    public List<String> findUrls(Long productId) {
        return productImageRepository.findByProductIdOrderBySortOrder(productId).stream()
                .map(ProductImage::getUrl)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findThumbnails(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productImageRepository.findByProductIdInOrderBySortOrder(productIds).stream()
                .collect(Collectors.toMap(image -> image.getProduct().getId(), ProductImage::getUrl, (first, second) -> first));
    }

    @Transactional
    public void deleteByUrls(Long productId, List<String> urls) {
        List<ProductImage> images = productImageRepository.findByProductIdAndUrlIn(productId, urls);
        productImageRepository.deleteAll(images);
        images.forEach(image -> s3ImageUploader.delete(image.getUrl()));
    }

    @Transactional
    public void deleteAll(Long productId) {
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        productImageRepository.deleteAll(images);
        images.forEach(image -> s3ImageUploader.delete(image.getUrl()));
    }
}
