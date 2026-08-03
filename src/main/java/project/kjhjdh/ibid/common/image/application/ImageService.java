package project.kjhjdh.ibid.common.image.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.image.domain.Image;
import project.kjhjdh.ibid.common.image.domain.ImageOwnerType;
import project.kjhjdh.ibid.common.image.infra.ImageRepository;
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.common.image.infra.S3ImageUploader;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final S3ImageUploader s3ImageUploader;

    public List<PresignedImage> presign(ImageOwnerType ownerType, Long ownerId, List<PresignTarget> targets) {
        String directory = ownerType.getDirectory() + "/" + ownerId;
        return targets.stream()
                .map(target -> {
                    PresignedUploadResult result =
                            s3ImageUploader.generatePresignedUrl(directory, target.filename(), target.contentType());
                    return new PresignedImage(result.presignedUrl(), result.key(), result.imageUrl());
                })
                .toList();
    }

    @Transactional
    public void attach(ImageOwnerType ownerType, Long ownerId, List<String> urls) {
        int base = imageRepository.countByOwnerTypeAndOwnerId(ownerType, ownerId);
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            images.add(Image.of(ownerType, ownerId, urls.get(i), base + i));
        }
        imageRepository.saveAll(images);
    }

    @Transactional(readOnly = true)
    public List<String> findUrls(ImageOwnerType ownerType, Long ownerId) {
        return imageRepository.findByOwnerTypeAndOwnerIdOrderBySortOrder(ownerType, ownerId).stream()
                .map(Image::getUrl)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findThumbnails(ImageOwnerType ownerType, List<Long> ownerIds) {
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return imageRepository.findByOwnerTypeAndOwnerIdInOrderBySortOrder(ownerType, ownerIds).stream()
                .collect(Collectors.toMap(Image::getOwnerId, Image::getUrl, (first, second) -> first));
    }

    @Transactional
    public void deleteByUrls(ImageOwnerType ownerType, Long ownerId, List<String> urls) {
        List<Image> images = imageRepository.findByOwnerTypeAndOwnerIdAndUrlIn(ownerType, ownerId, urls);
        imageRepository.deleteAll(images);
        images.forEach(image -> s3ImageUploader.delete(image.getUrl()));
    }

    @Transactional
    public void deleteAll(ImageOwnerType ownerType, Long ownerId) {
        List<Image> images = imageRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId);
        imageRepository.deleteAll(images);
        images.forEach(image -> s3ImageUploader.delete(image.getUrl()));
    }
}
