package com.loyce.omniflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LibraryPermission {
    // 指定方法参数中 libraryId 的参数名，默认为 "libraryId"
    String libraryIdParam() default "libraryId";
}