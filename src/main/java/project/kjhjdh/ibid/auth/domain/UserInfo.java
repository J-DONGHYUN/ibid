package project.kjhjdh.ibid.auth.domain;

import project.kjhjdh.ibid.user.domain.Role;

public record UserInfo(
        Long userId,
        Role role
) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
