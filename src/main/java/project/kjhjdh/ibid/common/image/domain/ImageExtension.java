package project.kjhjdh.ibid.common.image.domain;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

public enum ImageExtension {
    JPG,
    JPEG,
    PNG,
    GIF;

    public static ImageExtension fromUrl(String url) {
        int dot = url == null ? -1 : url.lastIndexOf('.');
        String ext = dot >= 0 ? url.substring(dot + 1).toLowerCase() : "";
        return switch (ext) {
            case "jpg" -> JPG;
            case "jpeg" -> JPEG;
            case "png" -> PNG;
            case "gif" -> GIF;
            default -> throw new BusinessException(ErrorCode.FILE_INVALID_EXTENSION);
        };
    }
}
