package org.mistakes.ratelimiter.core.annotation;

import java.lang.annotation.*;

/**
 * Duty: an annotation use redis to limit rate
 *
 * @author justafewmistakes
 * Date: 2022/02
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /**
     * a key in redis to limit rate
     * @return
     */
    String key() default "rate:limiter";

    /**
     * in expire time how many request can be handled
     * @return
     */
    long limit() default 10;

    /**
     * expire time , TIMEUNIT:second
     * @return
     */
    long expire() default 1;

    /**
     * message for beyond limit
     * @return
     */
    String message() default "Too many request, please wait for a while";
}
