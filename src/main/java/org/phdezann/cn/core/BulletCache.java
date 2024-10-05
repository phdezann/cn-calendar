package org.phdezann.cn.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.phdezann.cn.support.FileUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
public class BulletCache {

    private final AppArgs appArgs;
    private final JsonSerializer jsonSerializer;
    private final CacheContent cacheContent;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class CacheValue {
        private String eventId;
        private String bulletId;
        private String bulletTitle;
        private String bulletNote;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class CacheContent {
        private Map<String, CacheValue> entries = new HashMap<>();
    }

    public BulletCache(AppArgs appArgs, JsonSerializer jsonSerializer) {
        this.appArgs = appArgs;
        this.jsonSerializer = jsonSerializer;
        this.cacheContent = readOnDisk();
    }

    public Optional<BulletCache.CacheValue> get(String eventId) {
        return Optional.ofNullable(cacheContent.entries.get(eventId));
    }

    public void set(CacheValue cacheValue) {
        cacheContent.getEntries().put(cacheValue.getEventId(), cacheValue);
        persist();
    }

    private CacheContent readOnDisk() {
        var cacheFile = getCacheFile();
        if (cacheFile.exists()) {
            var json = FileUtils.read(cacheFile);
            return jsonSerializer.readValue(json, CacheContent.class);
        } else {
            return new CacheContent();
        }
    }

    private void persist() {
        var json = jsonSerializer.writeValue(cacheContent);
        FileUtils.write(getCacheFile(), json);
    }

    private File getCacheFile() {
        var cacheDir = appArgs.getCacheDirectory();
        FileUtils.forceMkdir(cacheDir);
        return new File(cacheDir, "events.json");
    }
}
