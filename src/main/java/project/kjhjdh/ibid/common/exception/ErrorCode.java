package project.kjhjdh.ibid.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Auth
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "존재하지 않는 리프레시 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "올바르지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "올바르지 않은 이메일 형식입니다."),
    INVALID_USERNAME_LENGTH(HttpStatus.BAD_REQUEST, "유저이름은 4자 이상 8자 이하여야 합니다."),
    INVALID_PASSWORD_LENGTH(HttpStatus.BAD_REQUEST, "비밀번호는 4자 이상 12자 이하여야 합니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    INVALID_PRODUCT_TITLE(HttpStatus.BAD_REQUEST, "상품 제목은 1자 이상 100자 이하여야 합니다."),
    INVALID_PRODUCT_DESCRIPTION(HttpStatus.BAD_REQUEST, "상품 설명은 1자 이상 2000자 이하여야 합니다."),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "판매가는 1원 이상이어야 합니다."),
    INVALID_PRODUCT_STOCK(HttpStatus.BAD_REQUEST, "재고 수량은 1개 이상이어야 합니다."),

    // Trade
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    SOLD_OUT(HttpStatus.CONFLICT, "품절된 상품입니다."),
    INVALID_PURCHASE_QUANTITY(HttpStatus.BAD_REQUEST, "구매 수량은 1개 이상이어야 합니다."),
    SELF_TRADE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인이 등록한 상품은 구매할 수 없습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String message;
}
