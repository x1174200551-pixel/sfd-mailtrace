package com.ntn.fziot.mailtrace.infrastructure.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在数据库写入前删除缓存，并在事务提交成功后延迟再次删除。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MtRedisCacheDoubleDelete {

    String cacheName();

    String key();

    long delayMillis() default 500;
}
