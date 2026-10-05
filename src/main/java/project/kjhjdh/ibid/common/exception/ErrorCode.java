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
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "요청을 처리할 수 없습니다."),
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
    PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "판매 중인 상품이 아닙니다."),
    PRODUCT_NOT_RESERVED(HttpStatus.CONFLICT, "예약 중인 상품이 아닙니다."),
    PRODUCT_ALREADY_SOLD(HttpStatus.CONFLICT, "이미 거래완료된 상품입니다."),
    PRODUCT_DELETED(HttpStatus.CONFLICT, "삭제된 상품입니다."),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT, "다른 요청이 먼저 상품 상태를 바꿨습니다. 다시 시도해주세요."),
    INVALID_DEVICE_CATEGORY(HttpStatus.BAD_REQUEST, "전자기기 카테고리를 선택해주세요."),
    INVALID_DEVICE_MODEL(HttpStatus.BAD_REQUEST, "모델명을 입력해주세요."),
    INVALID_BATTERY_HEALTH(HttpStatus.BAD_REQUEST, "배터리 성능은 0에서 100 사이여야 합니다."),
    INVALID_DEVICE_COMPONENTS(HttpStatus.BAD_REQUEST, "구성품을 입력해주세요."),

    // Chat
    CANNOT_OPEN_CHAT_ON_OWN_PRODUCT(HttpStatus.BAD_REQUEST, "본인 상품에는 채팅방을 열 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    INVALID_MESSAGE_CONTENT(HttpStatus.BAD_REQUEST, "메시지 내용은 1자 이상 1000자 이하여야 합니다."),
    INVALID_MESSAGE(HttpStatus.BAD_REQUEST, "메시지 식별자가 없습니다."),

    // Trade
    NOT_CHAT_PARTNER(HttpStatus.BAD_REQUEST, "그 상품의 채팅 상대가 아닙니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),

    // File
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "파일이 비어있습니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기는 10MB 이하여야 합니다."),
    FILE_INVALID_EXTENSION(HttpStatus.BAD_REQUEST, "jpg, jpeg, png, gif 형식만 업로드 가능합니다."),
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "이 상품으로 발급한 이미지 주소가 아닙니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String message;
}
