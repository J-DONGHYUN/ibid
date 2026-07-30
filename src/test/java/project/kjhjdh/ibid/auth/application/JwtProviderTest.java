package project.kjhjdh.ibid.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import project.kjhjdh.ibid.auth.domain.TokenClaims;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.exception.GlobalException;
import project.kjhjdh.ibid.user.domain.Role;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-0123456789-abcdefghij";
    private static final String OTHER_SECRET = "another-secret-key-0123456789-abcdef";

    @DisplayName("생성한 토큰에서 userId를 파싱한다")
    @Test
    void createAndParse() {
        // given
        JwtProvider provider = new JwtProvider(SECRET, 60_000L);
        String token = provider.createToken(42L, Role.USER);

        // when
        Long userId = provider.parseUserId(token);

        // then
        assertThat(userId).isEqualTo(42L);
    }

    @DisplayName("생성한 토큰에서 role claim을 파싱한다")
    @Test
    void parseClaims_role() {
        // given
        JwtProvider provider = new JwtProvider(SECRET, 60_000L);
        String token = provider.createToken(42L, Role.ADMIN);

        // when
        TokenClaims claims = provider.parseClaims(token);

        // then
        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.role()).isEqualTo(Role.ADMIN);
    }

    @DisplayName("만료된 토큰이면 파싱에 실패한다")
    @Test
    void parse_expired() {
        // given
        JwtProvider provider = new JwtProvider(SECRET, -1_000L);
        String expiredToken = provider.createToken(1L, Role.USER);

        // when & then
        assertThatThrownBy(() -> provider.parseUserId(expiredToken))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.EXPIRED_TOKEN.getMessage());
    }

    @DisplayName("서명이 일치하지 않으면 파싱에 실패한다")
    @Test
    void parse_wrongSignature() {
        // given
        JwtProvider issuer = new JwtProvider(SECRET, 60_000L);
        JwtProvider verifier = new JwtProvider(OTHER_SECRET, 60_000L);
        String token = issuer.createToken(1L, Role.USER);

        // when & then
        assertThatThrownBy(() -> verifier.parseUserId(token))
                .isInstanceOf(GlobalException.class)
                .hasMessage(ErrorCode.INVALID_TOKEN.getMessage());
    }

    @DisplayName("형식이 깨진 토큰이면 파싱에 실패한다")
    @Test
    void parse_malformed() {
        // given
        JwtProvider provider = new JwtProvider(SECRET, 60_000L);

        // when & then
        assertThatThrownBy(() -> provider.parseUserId("not-a-jwt"))
                .isInstanceOf(GlobalException.class)
                .hasMessage(ErrorCode.INVALID_TOKEN.getMessage());
    }
}
