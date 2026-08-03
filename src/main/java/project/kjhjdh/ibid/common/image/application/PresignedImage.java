package project.kjhjdh.ibid.common.image.application;

public record PresignedImage(
        String presignedUrl,
        String key,
        String imageUrl
) {
}
