package com.loyce.omniflow.aspect;

import com.loyce.omniflow.annotation.LibraryPermission;
import com.loyce.omniflow.common.biz.user.UserContext;
import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.service.LibraryService;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class LibraryPermissionAspect {

    @Resource
    private LibraryService libraryService;

    @Around("@annotation(com.loyce.omniflow.annotation.LibraryPermission)")
    public Object checkLibraryPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LibraryPermission annotation = method.getAnnotation(LibraryPermission.class);

        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        String libraryIdParamName = annotation.libraryIdParam();

        // 查找 libraryId参数值
        Long libraryId = getaLong(parameters, args, libraryIdParamName);

        // 获取当前登录用户ID（假设使用 Spring Security）
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new ClientException("用户未登录或无法获取用户信息");
        }

        // 校验当前用户是否有权限操作该库
        boolean hasPermission = libraryService.hasPermission(userId, libraryId);
        if (!hasPermission) {
            throw new ClientException("无权限操作该库: libraryId=" + libraryId);
        }

        // 权限校验通过，继续执行原方法
        return joinPoint.proceed();
    }

    private static Long getaLong(Parameter[] parameters, Object[] args, String libraryIdParamName) {
        Long libraryId = null;
        for (int i = 0; i < parameters.length; i++) {
            Object arg = args[i];
            if (arg == null) continue;

            // 参数名匹配（直接参数）
            if (parameters[i].getName().equals(libraryIdParamName)) {
                if (arg instanceof Integer) {
                    libraryId = ((Integer) arg).longValue();
                } else if (arg instanceof Long) {
                    libraryId = (Long) arg;
                }
                break;
            }

            // 参数为 DTO 时，从其中读取字段
            try {
                // 反射提取字段
                var field = arg.getClass().getDeclaredField(libraryIdParamName);
                field.setAccessible(true);
                Object fieldValue = field.get(arg);
                if (fieldValue instanceof Integer) {
                    libraryId = ((Integer) fieldValue).longValue();
                } else if (fieldValue instanceof Long) {
                    libraryId = (Long) fieldValue;
                }
                if (libraryId != null) break;
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }

        if (libraryId == null) {
            throw new ClientException("无法获取库ID参数: " + libraryIdParamName);
        }
        return libraryId;
    }

}
