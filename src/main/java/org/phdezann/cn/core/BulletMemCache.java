package org.phdezann.cn.core;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
public class BulletMemCache {

    private final Cache<String, CacheValue> cache = CacheBuilder //
            .newBuilder() //
            .expireAfterWrite(1, TimeUnit.DAYS) //
            .build();

    @RequiredArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class CacheValue {
        private final String title;
        private final String note;
    }

    public void put(String key, String title, String note) {
        cache.put(key, new CacheValue(title, note));
    }

    public Optional<CacheValue> get(String bulletId) {
        return Optional.ofNullable(cache.getIfPresent(bulletId));
    }

}
