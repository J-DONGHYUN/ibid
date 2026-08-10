package project.kjhjdh.ibid.auth.presentation.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import project.kjhjdh.ibid.auth.application.TokenProvider;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.exception.GlobalException;
import project.kjhjdh.ibid.user.domain.Role;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private AuthenticationInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private HandlerMethod handlerMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        handlerMethod = new HandlerMethod(new Object(), Object.class.getMethod("toString"));
    }

    @DisplayName("유효한 토큰이면 UserInfo를 request에 담고 통과시킨다")
    @Test
    void preHandle() {
        // given
        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenProvider.parseAccessToken("valid-token")).thenReturn(new UserInfo(7L, Role.USER));

        // when
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // then
        assertThat(result).isTrue();
        assertThat(request.getAttribute(AuthenticationInterceptor.USER_INFO_ATTRIBUTE))
                .isEqualTo(new UserInfo(7L, Role.USER));
    }

    @DisplayName("토큰이 없으면 인증에 실패한다")
    @Test
    void preHandle_noToken() {
        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(GlobalException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @DisplayName("토큰 파싱에 실패하면 해당 예외가 전파된다")
    @Test
    void preHandle_invalidToken() {
        // given
        request.addHeader("Authorization", "Bearer bad-token");
        when(tokenProvider.parseAccessToken("bad-token"))
                .thenThrow(new GlobalException(ErrorCode.INVALID_TOKEN));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(GlobalException.class)
                .hasMessage(ErrorCode.INVALID_TOKEN.getMessage());
    }

    @DisplayName("HandlerMethod가 아니면 인증 없이 통과시킨다")
    @Test
    void preHandle_nonHandlerMethod() {
        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        assertThat(request.getAttribute(AuthenticationInterceptor.USER_INFO_ATTRIBUTE)).isNull();
    }

    @DisplayName("OPTIONS 요청은 인증 없이 통과시킨다")
    @Test
    void preHandle_optionsRequest() {
        // given
        request.setMethod("OPTIONS");

        // when
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // then
        assertThat(result).isTrue();
        assertThat(request.getAttribute(AuthenticationInterceptor.USER_INFO_ATTRIBUTE)).isNull();
    }

    @DisplayName("@AdminOnly 엔드포인트는 관리자가 아니면 접근이 거부된다")
    @Test
    void preHandle_adminOnly_denied() throws NoSuchMethodException {
        // given
        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenProvider.parseAccessToken("valid-token")).thenReturn(new UserInfo(7L, Role.USER));
        HandlerMethod adminHandler = new HandlerMethod(new AdminHandler(), AdminHandler.class.getMethod("run"));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, adminHandler))
                .isInstanceOf(GlobalException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("@AdminOnly 엔드포인트는 관리자면 통과시킨다")
    @Test
    void preHandle_adminOnly_allowed() throws NoSuchMethodException {
        // given
        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenProvider.parseAccessToken("valid-token")).thenReturn(new UserInfo(9L, Role.ADMIN));
        HandlerMethod adminHandler = new HandlerMethod(new AdminHandler(), AdminHandler.class.getMethod("run"));

        // when
        boolean result = interceptor.preHandle(request, response, adminHandler);

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("@PublicApi 엔드포인트는 토큰이 없어도 통과시킨다")
    @Test
    void preHandle_publicApi_noToken() throws NoSuchMethodException {
        // given
        HandlerMethod publicHandler = new HandlerMethod(new PublicHandler(), PublicHandler.class.getMethod("run"));

        // when
        boolean result = interceptor.preHandle(request, response, publicHandler);

        // then
        assertThat(result).isTrue();
        assertThat(request.getAttribute(AuthenticationInterceptor.USER_INFO_ATTRIBUTE)).isNull();
    }

    @DisplayName("@PublicApi 엔드포인트에 유효한 토큰이 오면 UserInfo를 담아준다")
    @Test
    void preHandle_publicApi_validToken() throws NoSuchMethodException {
        // given
        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenProvider.parseAccessToken("valid-token")).thenReturn(new UserInfo(7L, Role.USER));
        HandlerMethod publicHandler = new HandlerMethod(new PublicHandler(), PublicHandler.class.getMethod("run"));

        // when
        boolean result = interceptor.preHandle(request, response, publicHandler);

        // then
        assertThat(result).isTrue();
        assertThat(request.getAttribute(AuthenticationInterceptor.USER_INFO_ATTRIBUTE))
                .isEqualTo(new UserInfo(7L, Role.USER));
    }

    @DisplayName("@PublicApi 엔드포인트라도 잘못된 토큰을 보내면 인증에 실패한다")
    @Test
    void preHandle_publicApi_invalidToken() throws NoSuchMethodException {
        // given
        request.addHeader("Authorization", "Bearer bad-token");
        when(tokenProvider.parseAccessToken("bad-token"))
                .thenThrow(new GlobalException(ErrorCode.INVALID_TOKEN));
        HandlerMethod publicHandler = new HandlerMethod(new PublicHandler(), PublicHandler.class.getMethod("run"));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, publicHandler))
                .isInstanceOf(GlobalException.class)
                .hasMessage(ErrorCode.INVALID_TOKEN.getMessage());
    }

    @AdminOnly
    static class AdminHandler {
        public void run() {
        }
    }

    static class PublicHandler {

        @PublicApi
        public void run() {
        }
    }
}
