package project.kjhjdh.ibid.auth.application;

public record LoginCommand(
        String email,
        String password
) {
}
