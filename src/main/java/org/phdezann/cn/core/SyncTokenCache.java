package org.phdezann.cn.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.phdezann.cn.support.FileUtils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SyncTokenCache {

    private final AppArgs appArgs;
    private final JsonSerializer jsonSerializer;
    private final CacheContent cacheContent;

    @NoArgsConstructor
    @Getter
    public static class CacheContent {
        private final Map<String, String> entries = new HashMap<>();
    }


    public SyncTokenCache(AppArgs appArgs, JsonSerializer jsonSerializer) {
        this.appArgs = appArgs;
        this.jsonSerializer = jsonSerializer;
        this.cacheContent = readOnDisk();
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

    public Optional<String> get(String key) {
        if (!cacheContent.getEntries().containsKey(key)) {
            return Optional.empty();
        }
        return Optional //
                .ofNullable(cacheContent.getEntries().get(key));
    }

    public void set(String key, String value) {
        cacheContent.getEntries().put(key, value);
        persist();
    }

    private void persist() {
        var json = jsonSerializer.writeValue(cacheContent);
        FileUtils.write(getCacheFile(), json);
    }

    private File getCacheFile() {
        var cacheDir = appArgs.getCacheDirectory();
        FileUtils.forceMkdir(cacheDir);
        return new File(cacheDir, "sync-token.json");
    }
}
