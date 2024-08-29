package org.phdezann.cn.core;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.phdezann.cn.core.Config.ConfigKey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
public class WorkflowyClient {

    private final AppArgs appArgs;
    private final Config config;
    private final JsonSerializer jsonSerializer;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class UpdateResult {
        private String id;
        private String result;
    }

    public UpdateResult createBullet(String name, String note) {
        return updateBullet(name, note, "");
    }

    public UpdateResult updateBullet(String name, String note, String itemId) {
        var url = buildUrl(name, note, itemId);
        var json = getContent(url);
        var result = jsonSerializer.readValue(json, UpdateResult.class);
        result = new UpdateResult(extractLastPartOfUUID(result.getId()), result.getResult());
        return result;
    }

    private String buildUrl(String name, String note, String itemId) {
        try {
            var port = config.get(ConfigKey.NODE_HTTP_SERVER_PORT);
            URIBuilder ub = new URIBuilder(String.format("http://localhost:%s/update", port));
            ub.addParameter("configFile", appArgs.getConfigFiles().iterator().next().getAbsolutePath());
            ub.addParameter("name", name);
            ub.addParameter("note", note);
            ub.addParameter("itemId", itemId);
            return ub.toString();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getContent(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder() //
                    .uri(new URI(url)) //
                    .GET() //
                    .build();
            return HttpClient.newBuilder().build().send(request, BodyHandlers.ofString()).body();
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String extractLastPartOfUUID(String bulletId) {
        if (!StringUtils.contains(bulletId, "-")) {
            return bulletId;
        }
        return substringAfterLast(bulletId, "-");
    }
}
