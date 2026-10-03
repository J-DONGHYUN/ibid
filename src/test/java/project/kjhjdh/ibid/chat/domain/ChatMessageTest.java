package project.kjhjdh.ibid.chat.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

class ChatMessageTest {

    @DisplayName("[CH-02] 빈 메시지는 만들 수 없다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_blankContent(String content) {
        // when & then
        assertThatThrownBy(() -> ChatMessage.create(1L, 20L, content, "c-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_MESSAGE_CONTENT.getMessage());
    }

    @DisplayName("[CH-02] 1000자 메시지는 만들 수 있고 1001자는 만들 수 없다 (QA-3.3 경계)")
    @Test
    void create_contentLengthBoundary() {
        // when & then — 상한 1000 자는 허용된다
        assertThatCode(() -> ChatMessage.create(1L, 20L, "가".repeat(1000), "c-1"))
                .doesNotThrowAnyException();

        // 1001 자는 거부된다. 컬럼 길이도 1000 이라 넘기면 INSERT 가 깨진다
        assertThatThrownBy(() -> ChatMessage.create(1L, 20L, "가".repeat(1001), "c-2"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_MESSAGE_CONTENT.getMessage());
    }

    @DisplayName("[CH-02] 클라이언트 메시지 식별자가 없으면 만들 수 없다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_blankClientMessageId(String clientMessageId) {
        // when & then
        assertThatThrownBy(() -> ChatMessage.create(1L, 20L, "안녕하세요", clientMessageId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_MESSAGE.getMessage());
    }
}
