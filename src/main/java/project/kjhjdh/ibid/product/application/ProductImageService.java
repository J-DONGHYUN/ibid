package project.kjhjdh.ibid.product.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.common.image.infra.S3ImageUploader;
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
                    PresignedUploadResult result = s3ImageUploader.generatePresignedUrl(directory, req.filename());
                    return new ImagePresignResponse(result.presignedUrl(), result.key(), result.imageUrl());
                })
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

    public void deleteFiles(List<String> urls) {
        urls.forEach(s3ImageUploader::delete);
    }
}
