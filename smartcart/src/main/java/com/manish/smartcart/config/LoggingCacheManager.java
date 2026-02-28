package com.manish.smartcart.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;

/**
 * A transparent decorator around RedisCacheManager that logs every
 * cache HIT, MISS, PUT, and EVICT with emoji prefixes for easy scanning.
 *
 * Wraps each Spring Cache with a LoggingCache proxy that intercepts
 * get() and put() calls without any business logic changes.
 */
@Slf4j
@RequiredArgsConstructor
public class LoggingCacheManager implements CacheManager {

    private final CacheManager delegate;

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        return cache != null ? new LoggingCache(cache) : null;
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    /**
     * Wraps a single Spring Cache and logs every operation.
     */
    @Slf4j
    static class LoggingCache implements Cache {

        private final Cache delegate;

        LoggingCache(Cache delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        /**
         * Called on every @Cacheable read.
         * Returns null → MISS. Returns value → HIT.
         */
        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper value = delegate.get(key);
            if (value != null) {
                log.info("✅ CACHE HIT  → [{}] :: key='{}'", delegate.getName(), key);
            } else {
                log.info("🔴 CACHE MISS → [{}] :: key='{}' | Querying database...", delegate.getName(), key);
            }
            return value;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T value = delegate.get(key, type);
            if (value != null) {
                log.info("✅ CACHE HIT  → [{}] :: key='{}'", delegate.getName(), key);
            } else {
                log.info("🔴 CACHE MISS → [{}] :: key='{}' | Querying database...", delegate.getName(), key);
            }
            return value;
        }

        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            // Spring calls this internally for @Cacheable in sync mode
            return delegate.get(key, valueLoader);
        }

        /**
         * Called after @Cacheable query completes — result is being stored.
         */
        @Override
        public void put(Object key, Object value) {
            log.info("💾 CACHE PUT  → [{}] :: key='{}' | Storing result in Redis", delegate.getName(), key);
            delegate.put(key, value);
        }

        /**
         * Called by @CacheEvict on writes (create/update/delete).
         */
        @Override
        public void evict(Object key) {
            log.info("🗑️  CACHE EVICT → [{}] :: key='{}'", delegate.getName(), key);
            delegate.evict(key);
        }

        @Override
        public void clear() {
            log.info("🗑️  CACHE CLEAR → [{}] :: All entries evicted", delegate.getName());
            delegate.clear();
        }
    }
}
