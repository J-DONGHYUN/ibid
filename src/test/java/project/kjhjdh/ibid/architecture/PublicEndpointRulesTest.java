package project.kjhjdh.ibid.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import project.kjhjdh.ibid.auth.presentation.WebConfig;

class PublicEndpointRulesTest {

    @DisplayName("경로 기반 공개 목록에는 /api/auth/** 하나만 있다.")
    @Test
    void publicEndpoints_onlyAuth() throws ReflectiveOperationException {
        Field field = WebConfig.class.getDeclaredField("PUBLIC_ENDPOINTS");
        field.setAccessible(true);

        String[] publicEndpoints = (String[]) field.get(null);

        assertThat(publicEndpoints)
                .as("공개 API 는 핸들러에 @PublicApi 를 붙여 연다. 경로 목록은 HTTP 메서드를 구분하지 못해 "
                        + "같은 경로의 수정 · 삭제까지 열린다 (ADR-0003)")
                .containsExactly("/api/auth/**");
    }
}
