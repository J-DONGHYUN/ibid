package project.kjhjdh.ibid.auth.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.TokenPair;
import project.kjhjdh.ibid.auth.presentation.dto.LoginRequest;
import project.kjhjdh.ibid.auth.presentation.dto.SignupRequest;
import project.kjhjdh.ibid.auth.infra.RefreshTokenRedisRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.exception.GlobalException;
import project.kjhjdh.ibid.user.domain.Email;
import project.kjhjdh.ibid.user.domain.User;
import project.kjhjdh.ibid.user.infra.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenValidator refreshTokenValidator;
    private final RefreshTokenRotator refreshTokenRotator;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Transactional
    public Long signup(SignupRequest request) {
        User.validateRawPassword(request.password());

        Email email = new Email(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), encodedPassword, request.username());

        return userRepository.save(user).getId();
    }

    @Transactional(readOnly = true)
    public TokenPair login(LoginRequest request) {
        User user = userRepository.findByEmail(new Email(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return tokenProvider.createTokenPair(user.getId(), user.getRole());
    }

    public TokenPair refresh(String oldToken) {
        Long userId = refreshTokenValidator.validate(oldToken);

        return refreshTokenRotator.rotate(userId);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null) {
            return;
        }

        try {
            Long userId = tokenProvider.parseRefreshToken(refreshToken);
            refreshTokenRedisRepository.deleteByUserId(userId);
        } catch (GlobalException e) {
            // 이미 만료/위조된 토큰이면 삭제할 대상이 없으므로 조용히 종료한다(로그아웃은 멱등).
        }
    }
}
