package com.ntn.fziot.mailtrace.infrastructure.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MtRedisCacheAspectTest {

    private final MtRedisCacheService cacheService = mock(MtRedisCacheService.class);
    private final TaskScheduler taskScheduler = mock(TaskScheduler.class);
    private final SampleService target = new SampleService();
    private final SampleService proxy = proxy(target);

    @AfterEach
    void cleanTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void cache_shouldReturnRedisValueWithoutCallingTarget() {
        when(cacheService.get(eq("sample"), eq("7"), any(Type.class)))
                .thenReturn(Optional.of(Set.of("cached")));

        Set<String> result = proxy.load(7L);

        assertEquals(Set.of("cached"), result);
        assertEquals(0, target.loadCount.get());
        verify(cacheService, never()).put(any(), any(), any(), any(Long.class));
    }

    @Test
    void cache_whenMiss_shouldCallTargetAndStoreValue() {
        when(cacheService.get(eq("sample"), eq("8"), any(Type.class))).thenReturn(Optional.empty());

        Set<String> result = proxy.load(8L);

        assertEquals(Set.of("db-8"), result);
        assertEquals(1, target.loadCount.get());
        verify(cacheService).put("sample", "8", result, 60);
    }

    @Test
    void cache_insideTransactionWhenHit_shouldReturnRedisValue() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        when(cacheService.get(eq("sample"), eq("9"), any(Type.class)))
                .thenReturn(Optional.of(Set.of("cached")));

        Set<String> result = proxy.load(9L);

        assertEquals(Set.of("cached"), result);
        assertEquals(0, target.loadCount.get());
        verify(cacheService, never()).put(any(), any(), any(), any(Long.class));
    }

    @Test
    void cache_insideTransactionWhenMiss_shouldPutOnlyAfterCommit() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        when(cacheService.get(eq("sample"), eq("9"), any(Type.class))).thenReturn(Optional.empty());

        Set<String> result = proxy.load(9L);

        assertEquals(Set.of("db-9"), result);
        verify(cacheService, never()).put(any(), any(), any(), any(Long.class));
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(cacheService).put("sample", "9", result, 60);
    }

    @Test
    void cache_insideTransactionWhenRolledBack_shouldNotPut() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        when(cacheService.get(eq("sample"), eq("9"), any(Type.class))).thenReturn(Optional.empty());

        proxy.load(9L);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        verify(cacheService, never()).put(any(), any(), any(), any(Long.class));
    }

    @Test
    void doubleDelete_shouldDeleteBeforeWriteAndScheduleSecondDelete() {
        proxy.save(10L, false);

        verify(cacheService).delete("sample", "10");
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(taskCaptor.capture(), any(Instant.class));
        taskCaptor.getValue().run();
        verify(cacheService, times(2)).delete("sample", "10");
    }

    @Test
    void doubleDelete_whenWriteFails_shouldNotScheduleSecondDelete() {
        assertThrows(IllegalStateException.class, () -> proxy.save(11L, true));

        verify(cacheService).delete("sample", "11");
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void doubleDelete_insideTransaction_shouldWaitForCommitBeforeScheduling() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        proxy.save(12L, false);

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    private SampleService proxy(SampleService service) {
        AspectJProxyFactory factory = new AspectJProxyFactory(service);
        factory.setProxyTargetClass(true);
        factory.addAspect(new MtRedisCacheAspect(cacheService, taskScheduler));
        return factory.getProxy();
    }

    static class SampleService {
        private final AtomicInteger loadCount = new AtomicInteger();

        @MtRedisCacheable(cacheName = "sample", key = "#id", ttlSeconds = 60)
        public Set<String> load(Long id) {
            loadCount.incrementAndGet();
            return Set.of("db-" + id);
        }

        @MtRedisCacheDoubleDelete(cacheName = "sample", key = "#id", delayMillis = 500)
        public void save(Long id, boolean fail) {
            if (fail) {
                throw new IllegalStateException("write failed");
            }
        }
    }
}
