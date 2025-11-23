package com.loyce.omniflow.common.enums;

import com.loyce.omniflow.common.convention.errorcode.IErrorCode;

public enum FileErrorCodeEnum implements IErrorCode {
    File_UPLOAD_ERROR("B000300", "文件上传出错");

    private final String code;
    private final String message;

    FileErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
