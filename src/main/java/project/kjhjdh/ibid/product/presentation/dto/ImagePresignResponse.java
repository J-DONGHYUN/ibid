package project.kjhjdh.ibid.product.presentation.dto;

public record ImagePresignResponse(
        String presignedUrl,
        String key,
        String imageUrl
) {
}
