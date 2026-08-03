package project.kjhjdh.ibid.common.image.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageOwnerType {
    PRODUCT("products"),
    USER_PROFILE("users");

    private final String directory;
}
