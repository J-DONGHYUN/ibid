package project.kjhjdh.ibid.auth.application;

import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import project.kjhjdh.ibid.auth.domain.TokenClaims;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.exception.GlobalException;
import project.kjhjdh.ibid.user.domain.Role;

public class JwtProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey secretKey;
    private final long expiration;

    public JwtProvider(String secret, long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }

    public String createToken(Long userId, Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public Long parseUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public TokenClaims parseClaims(String token) {
        Claims claims = getClaims(token);
        Long userId = Long.parseLong(claims.getSubject());
        Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
        return new TokenClaims(userId, role);
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
