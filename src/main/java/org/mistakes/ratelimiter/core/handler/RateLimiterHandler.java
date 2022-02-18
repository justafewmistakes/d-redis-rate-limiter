package org.mistakes.ratelimiter.core.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.mistakes.ratelimiter.core.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Duty: an aspect use redis to limit rate
 *
 * @author justafewmistakes
 * Date: 2022/02
 */
@Aspect
@Component
public class RateLimiterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterHandler.class);

    @Autowired
    RedisTemplate redisTemplate;

    private DefaultRedisScript<Long> getRedisScript;

    @PostConstruct
    public void init() {
        getRedisScript = new DefaultRedisScript<>();
        getRedisScript.setResultType(Long.class);
        getRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("rateLimiter.lua")));
        LOGGER.info("【load redis lua script success】");
    }

    @Pointcut("@annotation(org.mistakes.ratelimiter.core.annotation.RateLimiter)")
    public void rateLimiter1() {}

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, RateLimiter rateLimiter) throws Throwable {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("【aspect begin to work for limit rate】");
        }
        Signature signature = proceedingJoinPoint.getSignature();
        if (!(signature instanceof MethodSignature)) {
            throw new IllegalArgumentException("the Annotation @RateLimiter must used on method!");
        }

        String limitKey = rateLimiter.key();
        ObjectUtil.isNotNull(limitKey);
        long limitTimes = rateLimiter.limit();
        long expireTime = rateLimiter.expire();
        String message = rateLimiter.message();
        if (StrUtil.isBlank(message)) {
            message = "Too many request, please wait for a while";
        }

        Long result = (Long) redisTemplate.execute(getRedisScript, Collections.singletonList(limitKey), expireTime, limitTimes);
        if (result == null || result == 0) {
            LOGGER.error("expire time=" + expireTime + " and allow times =" + limitTimes
                    + ", and now there is too many request, please wait for a while");
            return message;
        }
        return proceedingJoinPoint.proceed();
    }
}
