package org.phdezann.cn.wf.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class HttpClient {

    public record Result(String html, List<String> cookies) {
    }

    public Result sendShareSecretLink(String shareSecretLink) {
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(toUri(shareSecretLink)) //
                .GET() //
                .build();
        var response = sendShareSecretLink(request);
        var html = response.body();
        var allCookies = response.headers().allValues("set-cookie");
        return new Result(html, allCookies);
    }

    public String sendGetInitializationData(String sessionId, String shareId) {
        var url = String.format("https://workflowy.com/get_initialization_data?share_id=%s" //
                + "&client_version=21" //
                + "&client_version_v2=28" //
                + "&no_root_children=1" //
                + "&include_main_tree=1", shareId);
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(toUri(url)) //
                .header("cookie", toSessionValue(sessionId)) //
                .GET() //
                .build();
        return sendShareSecretLink(request).body();
    }

    public String sendGetTreeData(String sessionId, String shareId) {
        var url = String.format("https://workflowy.com/get_tree_data/?share_id=%s", shareId);
        HttpRequest request = HttpRequest.newBuilder() //
                .header("cookie", toSessionValue(sessionId)) //
                .uri(toUri(url)) //
                .GET() //
                .build();
        return sendShareSecretLink(request).body();
    }

    @Builder
    public record PushAndPollArgs(String sessionId, //
            String shareId, //
            String pushPollData, //
            String clientId, //
            String uti, //
            String pushPollId) {
    }

    public String sendPushAndPoll(PushAndPollArgs args) {
        var url = String.format("https://workflowy.com/push_and_poll?uti=%s", args.uti());
        var httpEntity = MultipartEntityBuilder.create() //
                .addPart("client_id", toStringBody(args.clientId())) //
                .addPart("client_version", toStringBody("21")) //
                .addPart("push_poll_id", toStringBody(args.pushPollId())) //
                .addPart("push_poll_data", toStringBody(args.pushPollData())) //
                .addPart("share_id", toStringBody(args.shareId())) //
                .addPart("timezone", toStringBody("Europe/Paris")) //
                .build();
        var request = HttpRequest.newBuilder() //
                .uri(toUri(url)) //
                .header("cookie", toSessionValue(args.sessionId())) //
                .header("content-type", httpEntity.getContentType().getValue())
                .POST(BodyPublishers.ofInputStream(() -> toInputStream(httpEntity))) //
                .build();
        return sendShareSecretLink(request).body();
    }

    private InputStream toInputStream(HttpEntity httpEntity) {
        try {
            return httpEntity.getContent();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String toSessionValue(String sessionId) {
        return "sessionid=" + sessionId;
    }

    private StringBody toStringBody(String value) {
        var contentType = ContentType.create("application/x-www-form-urlencoded", StandardCharsets.UTF_8);
        return new StringBody(value, contentType);
    }

    private URI toUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    private HttpResponse<String> sendShareSecretLink(HttpRequest request) {
        try (var httpClient = java.net.http.HttpClient.newHttpClient()) {
            var bodyHandler = BodyHandlers.ofString(StandardCharsets.UTF_8);
            try {
                return httpClient.send(request, bodyHandler);
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
