package org.phdezann.cn.core;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.phdezann.cn.support.FileUtils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChannelCache {

    private final AppArgs appArgs;
    private final JsonSerializer jsonSerializer;
    private final CacheContent cacheContent;

    public record CacheValue(String calendarId, String resourceId, String channelId, ZonedDateTime expiration) {
    }

    @NoArgsConstructor
    @Getter
    public static class CacheContent {
        private final Map<String, CacheValue> entries = new HashMap<>();
    }

    public ChannelCache(AppArgs appArgs, JsonSerializer jsonSerializer) {
        this.appArgs = appArgs;
        this.jsonSerializer = jsonSerializer;
        this.cacheContent = readOnDisk();
    }

    public List<CacheValue> getAllValues() {
        return new ArrayList<>(cacheContent //
                .entries //
                .values());
    }

    public void set(CacheValue cacheValue) {
        var key = DigestUtils.md5Hex(cacheValue.calendarId());
        cacheContent.getEntries().put(key, cacheValue);
        persist();
    }

    public void clear() {
        cacheContent.getEntries().clear();
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
        return new File(cacheDir, "channel.json");
    }
}
