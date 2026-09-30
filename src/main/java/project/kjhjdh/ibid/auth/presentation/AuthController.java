package project.kjhjdh.ibid.auth.presentation;

import static project.kjhjdh.ibid.auth.presentation.cookie.RefreshTokenCookieHandler.REFRESH_TOKEN_COOKIE_NAME;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.application.AuthService;
import project.kjhjdh.ibid.auth.application.LoginCommand;
import project.kjhjdh.ibid.auth.application.SignupCommand;
import project.kjhjdh.ibid.auth.domain.TokenPair;
import project.kjhjdh.ibid.auth.presentation.cookie.RefreshTokenCookieHandler;
import project.kjhjdh.ibid.auth.presentation.dto.LoginRequest;
import project.kjhjdh.ibid.auth.presentation.dto.LoginResponse;
import project.kjhjdh.ibid.auth.presentation.dto.SignupRequest;
import project.kjhjdh.ibid.auth.presentation.dto.SignupResponse;
import project.kjhjdh.ibid.auth.presentation.dto.TokenRefreshResponse;
import project.kjhjdh.ibid.common.exception.ErrorResponse;
import project.kjhjdh.ibid.common.exception.GlobalException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieHandler cookieHandler;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        Long userId = authService.signup(new SignupCommand(request.email(), request.password(), request.username()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignupResponse(userId));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair result = authService.login(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieHandler.createRefreshTokenCookie(result.refreshToken()).toString())
                .body(new LoginResponse(result.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieHandler.removeRefreshTokenCookie().toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        try {
            TokenPair result = authService.refresh(refreshToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookieHandler.createRefreshTokenCookie(result.refreshToken()).toString())
                    .body(new TokenRefreshResponse(result.accessToken()));
        } catch (GlobalException e) {
            return ResponseEntity
                    .status(e.getErrorCode().getHttpStatus())
                    .header(HttpHeaders.SET_COOKIE, cookieHandler.removeRefreshTokenCookie().toString())
                    .body(ErrorResponse.of(e.getErrorCode()));
        }
    }
}
