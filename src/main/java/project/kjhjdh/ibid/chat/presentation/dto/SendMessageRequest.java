package project.kjhjdh.ibid.chat.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank(message = "메시지 내용을 입력해주세요.")
        String content,

        @NotBlank(message = "메시지 식별자를 입력해주세요.")
        String clientMessageId
) {
}
