package com.atguigu.daijia.common.login;

import cn.hutool.core.util.StrUtil;
import com.atguigu.daijia.common.constant.OtherConstant;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author guojiye
 * @description
 * @date 2025/5/27
 **/
@Component
@Aspect // 切面类
@RequiredArgsConstructor
public class LoginAspect {

    private final RedisTemplate redisTemplate;

    // 环绕通知，登录判断
    // 切入点表达式，指定对哪些规则的方法进行增强
    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(Login)")
    public Object login(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // 1 获取到request对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = sra.getRequest();

        // 2 从请求头获取token
        String token = request.getHeader(OtherConstant.CustomerTokenName);

        // 3 判断token是否为空，如果为空，则返回登录提示
        if (StrUtil.isBlank(token)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 4 token不为空，查询redis
        String userId = (String) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);

        // 5 查询redis对应用户id，把用户id放到ThreadLocal里面
        if (StrUtil.isNotBlank(userId)) {
            AuthContextHolder.setUserId(Long.parseLong(userId));
        }

        // 6 执行业务代码
        return proceedingJoinPoint.proceed();
    }

}
