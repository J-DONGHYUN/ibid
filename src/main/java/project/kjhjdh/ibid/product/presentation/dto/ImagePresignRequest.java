package project.kjhjdh.ibid.product.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record ImagePresignRequest(
        @NotBlank String filename,
        @NotBlank String contentType
) {
}
