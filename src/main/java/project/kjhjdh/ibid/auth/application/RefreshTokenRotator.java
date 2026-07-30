package project.kjhjdh.ibid.auth.application;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.TokenPair;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.user.domain.User;
import project.kjhjdh.ibid.user.infra.UserRepository;

@Component
@RequiredArgsConstructor
public class RefreshTokenRotator {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

    public TokenPair rotate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return tokenProvider.createTokenPair(userId, user.getRole());
    }
}
