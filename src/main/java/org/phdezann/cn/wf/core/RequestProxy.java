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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RequestProxy {

    private final HttpClient httpClient;
    private final JsonSerializer jsonSerializer;
    private final CreatePushPollOperationBuilder createPushPollOperationBuilder;
    private final EditPushPollOperationBuilder editPushPollOperationBuilder;

    public record CreateNodeResult(String sessionId, String shareId) {
    }

    public CreateNodeResult sendShareSecretLink(String shareSecretLink) {
        var response = httpClient.sendShareSecretLink(shareSecretLink);
        var sessionId = extractSessionId(response.cookies());
        var shareId = findShareId(response.html());
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

    @Builder
    public record CreateNodePushAndPollArgs(String transactionId, //
                                            Long dateJoinedTimestampInSeconds, //
                                            String rootId, //
                                            String shareId, //
                                            String sessionId, //
                                            String newNodeId, //
                                            String title, String note) {
    }

    public org.phdezann.cn.wf.json.push_and_poll.response.Root //
            sendCreateNodePushAndPoll(CreateNodePushAndPollArgs args) {
        var clientTimestamp = createClientTimestamp(args.dateJoinedTimestampInSeconds());
        var createPushPollOperationArgs = CreatePushPollOperationArgs.builder() //
                .nodeId(args.newNodeId()) //
                .parentNodeId(args.rootId()) //
                .shareId(args.shareId()) //
                .transactionId(args.transactionId()) //
                .clientTimeStamp(clientTimestamp) //
                .build();
        var editPushPollOperationArgs = EditPushPollOperationArgs.builder() //
                .nodeId(args.newNodeId()) //
                .shareId(args.shareId()) //
                .transactionId(args.transactionId()) //
                .clientTimeStamp(clientTimestamp) //
                .title(args.title()) //
                .note(args.note()) //
                .build();
        var createOperation = createPushPollOperationBuilder.build(createPushPollOperationArgs);
        var editOperation = editPushPollOperationBuilder.build(editPushPollOperationArgs);
        var root = new org.phdezann.cn.wf.json.push_and_poll.request.create.Root();
        root.setMostRecentOperationTransactionId(args.transactionId());
        root.setShareId(args.shareId());
        root.setOperations(List.of(createOperation, editOperation));
        var pushPollData = jsonSerializer.writeValue(List.of(root));
        var pushAndPollArgs = PushAndPollArgs.builder() //
                .sessionId(args.sessionId()) //
                .shareId(args.shareId()) //
                .pushPollData(pushPollData) //
                .clientId(generateUniqueClientId()) //
                .uti(generateUniqueUti()) //
                .pushPollId(generateRandomPushPollId()) //
                .build();
        var json = httpClient.sendPushAndPoll(pushAndPollArgs);
        return jsonSerializer.readValue(json, org.phdezann.cn.wf.json.push_and_poll.response.Root.class);
    }

    @Builder
    public record EditNodePushAndPollArgs(String transactionId, //
                                          Long dateJoinedTimestampInSeconds, //
                                          String nodeId, //
                                          String shareId, //
                                          String sessionId, //
                                          String title, //
                                          String note) {
    }

    public org.phdezann.cn.wf.json.push_and_poll.response.Root //
            sendEditNodePushAndPoll(EditNodePushAndPollArgs args) {
        var clientTimestamp = createClientTimestamp(args.dateJoinedTimestampInSeconds());
        var editPushPollOperationArgs = EditPushPollOperationArgs.builder() //
                .nodeId(args.nodeId()) //
                .shareId(args.shareId()) //
                .transactionId(args.transactionId()) //
                .clientTimeStamp(clientTimestamp) //
                .title(args.title()) //
                .note(args.note()) //
                .build();
        var editOperation = editPushPollOperationBuilder.build(editPushPollOperationArgs);
        var root = new org.phdezann.cn.wf.json.push_and_poll.request.edit.Root();
        root.setMostRecentOperationTransactionId(args.transactionId());
        root.setShareId(args.shareId());
        root.setOperations(List.of(editOperation));
        var pushPollData = jsonSerializer.writeValue(List.of(root));
        var pushAndPollArgs = PushAndPollArgs.builder() //
                .sessionId(args.sessionId()) //
                .shareId(args.shareId()) //
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
