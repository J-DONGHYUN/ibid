package project.kjhjdh.ibid.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private static final int USERNAME_MIN_LENGTH = 4;
    private static final int USERNAME_MAX_LENGTH = 8;
    private static final int PASSWORD_MIN_LENGTH = 4;
    private static final int PASSWORD_MAX_LENGTH = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Email email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private User(String email, String encodedPassword, String username, Role role) {
        validateUsername(username);
        this.email = new Email(email);
        this.password = encodedPassword;
        this.username = username;
        this.role = role;
    }

    public static User create(String email, String encodedPassword, String username) {
        return new User(email, encodedPassword, username, Role.USER);
    }

    public static User createAdmin(String email, String encodedPassword, String username) {
        return new User(email, encodedPassword, username, Role.ADMIN);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public static void validateRawPassword(String rawPassword) {
        if (rawPassword == null
                || rawPassword.length() < PASSWORD_MIN_LENGTH
                || rawPassword.length() > PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_LENGTH);
        }
    }

    private void validateUsername(String username) {
        if (username == null
                || username.length() < USERNAME_MIN_LENGTH
                || username.length() > USERNAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME_LENGTH);
        }
    }
}
