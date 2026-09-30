package project.kjhjdh.ibid.auth.application;

public record SignupCommand(
        String email,
        String password,
        String username
) {
}
