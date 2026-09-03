package com.ntn.fziot.mailtrace.infrastructure.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为公开查询方法增加 Redis 缓存。key 支持 Spring EL，例如 {@code #roleId}。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MtRedisCacheable {

    String cacheName();

    String key();

    long ttlSeconds() default 600;
}
