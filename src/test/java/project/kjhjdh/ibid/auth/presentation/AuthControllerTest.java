package project.kjhjdh.ibid.auth.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.auth.application.AuthService;
import project.kjhjdh.ibid.auth.domain.TokenPair;
import project.kjhjdh.ibid.auth.presentation.cookie.RefreshTokenCookieHandler;
import project.kjhjdh.ibid.auth.presentation.dto.LoginRequest;
import project.kjhjdh.ibid.auth.presentation.dto.SignupRequest;
import project.kjhjdh.ibid.auth.presentation.interceptor.AuthenticationInterceptor;
import project.kjhjdh.ibid.common.config.WebConfig;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.exception.GlobalException;
import project.kjhjdh.ibid.support.ControllerTestSupport;

@ActiveProfiles("test")
@Import(RefreshTokenCookieHandler.class)
@WebMvcTest(controllers = AuthController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {WebConfig.class, AuthenticationInterceptor.class}))
class AuthControllerTest extends ControllerTestSupport {

    @MockitoBean
    private AuthService authService;

    @DisplayName("회원가입에 성공하면 201과 userId를 응답한다")
    @Test
    void signup() {
        // given
        given(authService.signup(any())).willReturn(1L);

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new SignupRequest("user@example.com", "password", "tester"))
                .when()
                .post("/api/auth/signup")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("userId", equalTo(1));
    }

    @DisplayName("이메일 형식이 올바르지 않으면 400을 응답한다")
    @Test
    void signup_invalidEmail() {
        // given
        SignupRequest request = new SignupRequest("not-an-email", "password", "tester");

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/auth/signup")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("비밀번호 길이가 유효하지 않으면 400을 응답한다")
    @Test
    void signup_invalidPassword() {
        // given
        SignupRequest request = new SignupRequest("user@example.com", "1", "tester");

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/auth/signup")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("요청 본문이 올바른 JSON이 아니면 400을 응답한다")
    @Test
    void signup_malformedJson() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{ not-json ")
                .when()
                .post("/api/auth/signup")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("이미 가입된 이메일이면 400과 DUPLICATE_EMAIL을 응답한다")
    @Test
    void signup_duplicateEmail() {
        // given
        given(authService.signup(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new SignupRequest("user@example.com", "password", "tester"))
                .when()
                .post("/api/auth/signup")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("DUPLICATE_EMAIL"));
    }

    @DisplayName("로그인에 성공하면 accessToken과 refresh_token 쿠키를 응답한다")
    @Test
    void login() {
        // given
        given(authService.login(any())).willReturn(new TokenPair("access-token", "refresh-token"));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("user@example.com", "password"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", equalTo("access-token"))
                .header(HttpHeaders.SET_COOKIE, containsString(RefreshTokenCookieHandler.REFRESH_TOKEN_COOKIE_NAME));
    }

    @DisplayName("로그인에 실패하면 401과 LOGIN_FAILED를 응답한다")
    @Test
    void login_failed() {
        // given
        given(authService.login(any())).willThrow(new BusinessException(ErrorCode.LOGIN_FAILED));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("user@example.com", "password"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("LOGIN_FAILED"));
    }

    @DisplayName("로그아웃하면 200과 함께 refresh_token 쿠키를 만료시킨다")
    @Test
    void logout() {
        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(HttpStatus.OK.value())
                .header(HttpHeaders.SET_COOKIE, containsString("Max-Age=0"));
    }

    @DisplayName("리프레시에 성공하면 새 accessToken과 refresh_token 쿠키를 응답한다")
    @Test
    void refresh() {
        // given
        given(authService.refresh(any())).willReturn(new TokenPair("new-access-token", "new-refresh-token"));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", equalTo("new-access-token"))
                .header(HttpHeaders.SET_COOKIE, containsString(RefreshTokenCookieHandler.REFRESH_TOKEN_COOKIE_NAME));
    }

    @DisplayName("유효하지 않은 리프레시 토큰이면 401과 함께 쿠키를 제거한다")
    @Test
    void refresh_invalidToken() {
        // given
        given(authService.refresh(any())).willThrow(new GlobalException(ErrorCode.INVALID_REFRESH_TOKEN));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("INVALID_REFRESH_TOKEN"))
                .header(HttpHeaders.SET_COOKIE, containsString("Max-Age=0"));
    }
}
