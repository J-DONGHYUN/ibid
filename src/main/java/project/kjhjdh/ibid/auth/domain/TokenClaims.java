package project.kjhjdh.ibid.auth.domain;

import project.kjhjdh.ibid.user.domain.Role;

public record TokenClaims(Long userId, Role role) {
}
