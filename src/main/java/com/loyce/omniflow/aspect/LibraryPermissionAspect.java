package com.loyce.omniflow.aspect;

import com.loyce.omniflow.annotation.CheckLibraryPermission;
import com.loyce.omniflow.common.biz.user.UserContext;
import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.service.LibraryService;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 库权限校验切面
 * 使用 SpEL 解析 libraryId 并校验用户权限
 */
@Aspect
@Component
public class LibraryPermissionAspect {

    @Resource
    private LibraryService libraryService;

    @Resource
    private ApplicationContext applicationContext;

    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(com.loyce.omniflow.annotation.CheckLibraryPermission)")
    public Object checkLibraryPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CheckLibraryPermission annotation = method.getAnnotation(CheckLibraryPermission.class);

        // 1. 构造SpEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        // 方法参数名 -> 参数值
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        // 允许通过 @beanName 调用 Spring Bean
        context.setBeanResolver(new BeanFactoryResolver(applicationContext));

        // 2. 解析SpEL，得到libraryId
        Object value;
        try {
            value = parser.parseExpression(annotation.libraryId()).getValue(context);
        } catch (Exception e) {
            throw new ClientException("权限注解解析 libraryId 失败: " + e.getMessage());
        }
        if (!(value instanceof Number)) {
            throw new ClientException("权限注解解析 libraryId 失败，返回值不是数字");
        }
        Long libraryId = ((Number) value).longValue();

        // 3. 权限校验
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
}
