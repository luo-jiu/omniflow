package com.loyce.omniflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 库权限校验注解
 * 支持 SpEL 表达式解析 libraryId
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckLibraryPermission {
    String libraryId();
}
