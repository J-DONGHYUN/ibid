package project.kjhjdh.ibid.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import project.kjhjdh.ibid.auth.domain.TokenPair;
import project.kjhjdh.ibid.auth.infra.RefreshTokenRedisRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.support.IntegrationTestSupport;
import project.kjhjdh.ibid.user.domain.Email;
import project.kjhjdh.ibid.user.domain.User;
import project.kjhjdh.ibid.user.infra.UserRepository;

class AuthServiceTest extends IntegrationTestSupport {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @DisplayName("회원가입하면 비밀번호가 인코딩되어 저장된다")
    @Test
    void signup() {
        // given
        SignupCommand request = new SignupCommand("new@example.com", "password", "tester");

        // when
        Long userId = authService.signup(request);

        // then
        User saved = userRepository.findByEmail(new Email("new@example.com")).orElseThrow();
        assertThat(saved.getId()).isEqualTo(userId);
        assertThat(saved.getPassword()).isNotEqualTo("password");
        assertThat(passwordEncoder.matches("password", saved.getPassword())).isTrue();
    }

    @DisplayName("이미 가입된 이메일이면 회원가입에 실패한다")
    @Test
    void signup_duplicateEmail() {
        // given
        authService.signup(new SignupCommand("dup@example.com", "password", "tester"));
        SignupCommand duplicate = new SignupCommand("dup@example.com", "password", "other");

        // when & then
        assertThatThrownBy(() -> authService.signup(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.DUPLICATE_EMAIL.getMessage());
    }

    @DisplayName("비밀번호 길이가 유효하지 않으면 회원가입에 실패한다")
    @Test
    void signup_invalidPassword() {
        // given
        SignupCommand request = new SignupCommand("bad@example.com", "1", "tester");

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PASSWORD_LENGTH.getMessage());
    }

    @DisplayName("로그인에 성공하면 토큰 쌍을 발급한다")
    @Test
    void login() {
        // given
        authService.signup(new SignupCommand("login@example.com", "password", "tester"));
        LoginCommand request = new LoginCommand("login@example.com", "password");

        // when
        TokenPair tokenPair = authService.login(request);

        // then
        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).isNotBlank();
    }

    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    @Test
    void login_wrongPassword() {
        // given
        authService.signup(new SignupCommand("login@example.com", "password", "tester"));
        LoginCommand request = new LoginCommand("login@example.com", "wrong-password");

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
    }

    @DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
    @Test
    void login_notFound() {
        // given
        LoginCommand request = new LoginCommand("nobody@example.com", "password");

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.LOGIN_FAILED.getMessage());
    }

    @DisplayName("로그아웃하면 저장된 리프레시 토큰이 삭제된다")
    @Test
    void logout() {
        // given
        Long userId = authService.signup(new SignupCommand("logout@example.com", "password", "tester"));
        TokenPair tokenPair = authService.login(new LoginCommand("logout@example.com", "password"));

        // when
        authService.logout(tokenPair.refreshToken());

        // then
        assertThat(refreshTokenRedisRepository.findTokenByUserId(userId)).isEmpty();
    }

    @DisplayName("만료·위조된 리프레시 토큰으로 로그아웃해도 예외가 발생하지 않는다")
    @Test
    void logout_invalidToken() {
        // when & then
        assertThatCode(() -> authService.logout("invalid-token")).doesNotThrowAnyException();
    }
}
