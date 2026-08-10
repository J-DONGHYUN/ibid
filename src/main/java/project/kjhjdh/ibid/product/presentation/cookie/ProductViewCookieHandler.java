package project.kjhjdh.ibid.product.presentation.cookie;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class ProductViewCookieHandler {

    public static final String VISITOR_ID_COOKIE_NAME = "visitor_id";

    private final long cookieMaxAge;

    public ProductViewCookieHandler(@Value("${product.view.cookie-max-age}") long cookieMaxAge) {
        this.cookieMaxAge = cookieMaxAge;
    }

    public String resolveVisitorId(String visitorId) {
        if (visitorId == null || visitorId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return visitorId;
    }

    public ResponseCookie createVisitorIdCookie(String visitorId) {
        return ResponseCookie.from(VISITOR_ID_COOKIE_NAME, visitorId)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(cookieMaxAge)
                .sameSite("Strict")
                .build();
    }
}
