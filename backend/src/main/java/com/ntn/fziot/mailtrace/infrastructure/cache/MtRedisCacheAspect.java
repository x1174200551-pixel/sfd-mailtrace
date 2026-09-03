package com.ntn.fziot.mailtrace.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MtRedisCacheAspect {

    private final MtRedisCacheService cacheService;
    private final TaskScheduler taskScheduler;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public MtRedisCacheAspect(MtRedisCacheService cacheService,
                              @Qualifier("mtRedisCacheTaskScheduler") TaskScheduler taskScheduler) {
        this.cacheService = cacheService;
        this.taskScheduler = taskScheduler;
    }

    @Around("@annotation(com.ntn.fziot.mailtrace.infrastructure.cache.MtRedisCacheable)")
    public Object cache(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = resolveMethod(joinPoint);
        MtRedisCacheable cacheable = AnnotatedElementUtils.findMergedAnnotation(method, MtRedisCacheable.class);
        if (cacheable == null) {
            return joinPoint.proceed();
        }
        String businessKey = evaluateKey(joinPoint, method, cacheable.key());
        if (businessKey == null) {
            return joinPoint.proceed();
        }

        Optional<Object> cached = cacheService.get(cacheable.cacheName(), businessKey, method.getGenericReturnType());
        if (cached.isPresent()) {
            return cached.get();
        }

        Object value = joinPoint.proceed();
        putAfterCommit(cacheable, businessKey, value);
        return value;
    }

    @Around("@annotation(com.ntn.fziot.mailtrace.infrastructure.cache.MtRedisCacheDoubleDelete)")
    public Object doubleDelete(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = resolveMethod(joinPoint);
        MtRedisCacheDoubleDelete doubleDelete = AnnotatedElementUtils.findMergedAnnotation(
                method, MtRedisCacheDoubleDelete.class);
        if (doubleDelete == null) {
            return joinPoint.proceed();
        }
        String businessKey = evaluateKey(joinPoint, method, doubleDelete.key());
        if (businessKey == null) {
            return joinPoint.proceed();
        }

        cacheService.delete(doubleDelete.cacheName(), businessKey);
        Object result = joinPoint.proceed();
        Runnable secondDelete = () -> scheduleDelete(doubleDelete, businessKey);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    secondDelete.run();
                }
            });
        } else {
            secondDelete.run();
        }
        return result;
    }

    private void scheduleDelete(MtRedisCacheDoubleDelete annotation, String businessKey) {
        Runnable deleteTask = () -> cacheService.delete(annotation.cacheName(), businessKey);
        if (annotation.delayMillis() <= 0) {
            deleteTask.run();
            return;
        }
        try {
            taskScheduler.schedule(deleteTask, Instant.now().plusMillis(annotation.delayMillis()));
        } catch (RuntimeException exception) {
            log.warn("调度 Redis 延迟删除失败，改为立即删除，cacheName={}", annotation.cacheName(), exception);
            deleteTask.run();
        }
    }

    private void putAfterCommit(MtRedisCacheable annotation, String businessKey, Object value) {
        Runnable cachePut = () -> cacheService.put(
                annotation.cacheName(), businessKey, value, annotation.ttlSeconds());
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            cachePut.run();
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.debug("当前事务未启用同步，跳过 Redis 缓存写入，cacheName={}", annotation.cacheName());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cachePut.run();
            }
        });
    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return AopUtils.getMostSpecificMethod(method, joinPoint.getTarget().getClass());
    }

    private String evaluateKey(ProceedingJoinPoint joinPoint, Method method, String expression) {
        try {
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(), method, joinPoint.getArgs(), parameterNameDiscoverer);
            Object value = expressionParser.parseExpression(expression).getValue(context);
            if (value == null || value.toString().isBlank()) {
                return null;
            }
            return value.toString();
        } catch (RuntimeException exception) {
            log.warn("解析 Redis 缓存 key 失败，已跳过缓存，method={}", method.getName(), exception);
            return null;
        }
    }
}
