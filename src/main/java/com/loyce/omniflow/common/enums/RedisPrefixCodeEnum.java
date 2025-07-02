package com.loyce.omniflow.common.enums;


import lombok.Getter;

@Getter
public enum RedisPrefixCodeEnum {
    USER_LOGIN_CODING("login:");

    private final String val;

    RedisPrefixCodeEnum(String val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return val;
    }
}
