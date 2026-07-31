package project.kjhjdh.ibid.auth.presentation.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.interceptor.AuthenticationInterceptor;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.exception.GlobalException;
import project.kjhjdh.ibid.user.domain.Role;

class LoginUserArgumentResolverTest {

    private final LoginUserArgumentResolver resolver = new LoginUserArgumentResolver();

    @DisplayName("@LoginUser가 붙은 UserInfo 파라미터를 지원한다")
    @Test
    void supportsParameter() throws NoSuchMethodException {
        // given
        MethodParameter parameter = methodParameter("withLoginUser");

        // when & then
        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @DisplayName("애노테이션이 없으면 지원하지 않는다")
    @Test
    void supportsParameter_noAnnotation() throws NoSuchMethodException {
        // given
        MethodParameter parameter = methodParameter("withoutAnnotation");

        // when & then
        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @DisplayName("request에 담긴 UserInfo를 주입한다")
    @Test
    void resolveArgument() throws NoSuchMethodException {
        // given
        UserInfo userInfo = new UserInfo(3L, Role.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthenticationInterceptor.USER_INFO_ATTRIBUTE, userInfo);

        // when
        Object resolved = resolver.resolveArgument(
                methodParameter("withLoginUser"), null, new ServletWebRequest(request), null);

        // then
        assertThat(resolved).isEqualTo(userInfo);
    }

    @DisplayName("UserInfo가 없으면 인증에 실패한다")
    @Test
    void resolveArgument_absent() throws NoSuchMethodException {
        // given
        ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());
        MethodParameter parameter = methodParameter("withLoginUser");

        // when & then
        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, webRequest, null))
                .isInstanceOf(GlobalException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    private MethodParameter methodParameter(String methodName) throws NoSuchMethodException {
        return new MethodParameter(Target.class.getDeclaredMethod(methodName, UserInfo.class), 0);
    }

    @SuppressWarnings("unused")
    static class Target {

        void withLoginUser(@LoginUser UserInfo user) {
        }

        void withoutAnnotation(UserInfo user) {
        }
    }
}
