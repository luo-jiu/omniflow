package com.loyce.omniflow.common.enums;

public enum NodeTypeEnum {
    FOLDER(0, "dir"),
    FILE(1, "file");

    private final int code;
    private final String desc;

    NodeTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static NodeTypeEnum fromCode(int code) {
        for (NodeTypeEnum type : values()) {
            if (type.code == code) return type;
        }
        return null;
    }
}
