package project.kjhjdh.ibid.chat.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
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
}
