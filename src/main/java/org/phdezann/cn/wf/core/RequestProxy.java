package org.phdezann.cn.wf.core;

import static org.phdezann.cn.wf.core.InitialHtmlPageUtils.extractSessionId;
import static org.phdezann.cn.wf.core.InitialHtmlPageUtils.findShareId;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.JsonSerializer;
import org.phdezann.cn.wf.core.HttpClient.PushAndPollArgs;
import org.phdezann.cn.wf.core.request.CreatePushPollOperationBuilder;
import org.phdezann.cn.wf.core.request.CreatePushPollOperationBuilder.CreatePushPollOperationArgs;
import org.phdezann.cn.wf.core.request.EditPushPollOperationBuilder;
import org.phdezann.cn.wf.core.request.EditPushPollOperationBuilder.EditPushPollOperationArgs;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
public class RequestProxy {

    private final HttpClient httpClient;
    private final JsonSerializer jsonSerializer;
    private final CreatePushPollOperationBuilder createPushPollOperationBuilder;
    private final EditPushPollOperationBuilder editPushPollOperationBuilder;

    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class CreateNodeResult {
        private final String sessionId;
        private final String shareId;
    }

    public CreateNodeResult sendShareSecretLink(String shareSecretLink) {
        var response = httpClient.sendShareSecretLink(shareSecretLink);
        var sessionId = extractSessionId(response.getCookies());
        var shareId = findShareId(response.getHtml());
        return new CreateNodeResult(sessionId, shareId);
    }

    public org.phdezann.cn.wf.json.get_initialization_data.Root sendGetInitializationData(String sessionId,
            String shareId) {
        var json = httpClient.sendGetInitializationData(sessionId, shareId);
        return jsonSerializer.readValue(json, org.phdezann.cn.wf.json.get_initialization_data.Root.class);
    }

    public org.phdezann.cn.wf.json.get_tree_data.Root sendGetTreeData(String sessionId, String shareId) {
        var json = httpClient.sendGetTreeData(sessionId, shareId);
        return jsonSerializer.readValue(json, org.phdezann.cn.wf.json.get_tree_data.Root.class);
    }

    @RequiredArgsConstructor
    @Builder
    @Getter
    public static class CreateNodePushAndPollArgs {
        @lombok.NonNull
        private final String transactionId;
        @lombok.NonNull
        private final Long dateJoinedTimestampInSeconds;
        @lombok.NonNull
        private final String rootId;
        @lombok.NonNull
        private final String shareId;
        @lombok.NonNull
        private final String sessionId;
        @lombok.NonNull
        private final String newNodeId;
        @lombok.NonNull
        private final String title;
        @lombok.NonNull
        private final String note;
    }

    public org.phdezann.cn.wf.json.push_and_poll.response.Root //
            sendCreateNodePushAndPoll(CreateNodePushAndPollArgs args) {
        var clientTimestamp = createClientTimestamp(args.getDateJoinedTimestampInSeconds());
        var createPushPollOperationArgs = CreatePushPollOperationArgs.builder() //
                .nodeId(args.getNewNodeId()) //
                .parentNodeId(args.getRootId()) //
                .shareId(args.getShareId()) //
                .transactionId(args.getTransactionId()) //
                .clientTimeStamp(clientTimestamp) //
                .build();
        var editPushPollOperationArgs = EditPushPollOperationArgs.builder() //
                .nodeId(args.getNewNodeId()) //
                .shareId(args.getShareId()) //
                .transactionId(args.getTransactionId()) //
                .clientTimeStamp(clientTimestamp) //
                .title(args.getTitle()) //
                .note(args.getNote()) //
                .build();
        var createOperation = createPushPollOperationBuilder.build(createPushPollOperationArgs);
        var editOperation = editPushPollOperationBuilder.build(editPushPollOperationArgs);
        var root = new org.phdezann.cn.wf.json.push_and_poll.request.create.Root();
        root.setMostRecentOperationTransactionId(args.getTransactionId());
        root.setShareId(args.getShareId());
        root.setOperations(List.of(createOperation, editOperation));
        var pushPollData = jsonSerializer.writeValue(List.of(root));
        var pushAndPollArgs = PushAndPollArgs.builder() //
                .sessionId(args.getSessionId()) //
                .shareId(args.getShareId()) //
                .pushPollData(pushPollData) //
                .clientId(generateUniqueClientId()) //
                .uti(generateUniqueUti()) //
                .pushPollId(generateRandomPushPollId()) //
                .build();
        var json = httpClient.sendPushAndPoll(pushAndPollArgs);
        return jsonSerializer.readValue(json, org.phdezann.cn.wf.json.push_and_poll.response.Root.class);
    }

    @RequiredArgsConstructor
    @Builder
    @Getter
    public static class EditNodePushAndPollArgs {
        @lombok.NonNull
        private final String transactionId;
        @lombok.NonNull
        private final Long dateJoinedTimestampInSeconds;
        @lombok.NonNull
        private final String nodeId;
        @lombok.NonNull
        private final String shareId;
        @lombok.NonNull
        private final String sessionId;
        @lombok.NonNull
        private final String title;
        @lombok.NonNull
        private final String note;
    }

    public org.phdezann.cn.wf.json.push_and_poll.response.Root //
            sendEditNodePushAndPoll(EditNodePushAndPollArgs args) {
        var clientTimestamp = createClientTimestamp(args.getDateJoinedTimestampInSeconds());
        var editPushPollOperationArgs = EditPushPollOperationArgs.builder() //
                .nodeId(args.getNodeId()) //
                .shareId(args.getShareId()) //
                .transactionId(args.getTransactionId()) //
                .clientTimeStamp(clientTimestamp) //
                .title(args.getTitle()) //
                .note(args.getNote()) //
                .build();
        var editOperation = editPushPollOperationBuilder.build(editPushPollOperationArgs);
        var root = new org.phdezann.cn.wf.json.push_and_poll.request.edit.Root();
        root.setMostRecentOperationTransactionId(args.getTransactionId());
        root.setShareId(args.getShareId());
        root.setOperations(List.of(editOperation));
        var pushPollData = jsonSerializer.writeValue(List.of(root));
        var pushAndPollArgs = PushAndPollArgs.builder() //
                .sessionId(args.getSessionId()) //
                .shareId(args.getShareId()) //
                .pushPollData(pushPollData) //
                .clientId(generateUniqueClientId()) //
                .uti(generateUniqueUti()) //
                .pushPollId(generateRandomPushPollId()) //
                .build();
        var json = httpClient.sendPushAndPoll(pushAndPollArgs);
        return jsonSerializer.readValue(json, org.phdezann.cn.wf.json.push_and_poll.response.Root.class);
    }

    private String generateUniqueUti() {
        long epochUTC = Instant.now().toEpochMilli();
        var pushPollId = RandomStringUtils.randomAlphanumeric(8);
        return String.format("unknown-%d-%s", epochUTC, pushPollId);
    }

    private String generateUniqueClientId() {
        var utc = ZonedDateTime.now(ZoneId.of("UTC"));
        var dateTime = utc.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.n"));
        return StringUtils.substring(dateTime, 0, dateTime.length() - 3);
    }

    private String generateRandomPushPollId() {
        return RandomStringUtils.randomAlphanumeric(8);
    }

    private long createClientTimestamp(long dateJoinedTimestampInSeconds) {
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        long epoch = LocalDateTime.now().atZone(zoneId).toEpochSecond();
        return epoch - dateJoinedTimestampInSeconds;
    }

}
