package org.phdezann.cn.wf.core;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.Config;
import org.phdezann.cn.core.Config.ConfigKey;
import org.phdezann.cn.wf.core.RequestProxy.CreateNodePushAndPollArgs;
import org.phdezann.cn.wf.core.RequestProxy.EditNodePushAndPollArgs;
import org.phdezann.cn.wf.json.push_and_poll.response.Root;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

public class WorkflowyClient {

    private final RequestProxy requestProxy;

    private String shareId;
    private String shareNodeRootId;
    private String sessionId;
    private long dateJoinedTimestampInSeconds;
    private WorkflowyStateUpdater workflowyStateUpdater;

    public WorkflowyClient(Config config, RequestProxy requestProxy) {
        this.requestProxy = requestProxy;
        open(config.get(ConfigKey.WORKFLOWY_SHARE_SECRET_LINK));
    }

    public void open(String shareSecretLink) {
        var response = requestProxy.sendShareSecretLink(shareSecretLink);
        sessionId = response.getSessionId();
        shareId = response.getShareId();
        var initializationData = requestProxy.sendGetInitializationData(sessionId, shareId);
        dateJoinedTimestampInSeconds = initializationData //
                .getProjectTreeData() //
                .getAuxiliaryProjectTreeInfos() //
                .iterator() //
                .next() //
                .getDateJoinedTimestampInSeconds();
        workflowyStateUpdater = new WorkflowyStateUpdater(requestProxy, shareId, sessionId);
        shareNodeRootId = workflowyStateUpdater.getNodeRootId();
    }

    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class Result {
        private final String nodeShortId;
    }

    public Result createNode(String title, String note) {
        return createNode(title, note, true);
    }

    public Result createNode(String title, String note, boolean updateState) {
        if (updateState) {
            workflowyStateUpdater.updateState();
        }
        var newNodeId = generateRandomNodeId();
        var args = CreateNodePushAndPollArgs.builder() //
                .transactionId(workflowyStateUpdater.getMostRecentOperationTransactionId()) //
                .dateJoinedTimestampInSeconds(dateJoinedTimestampInSeconds) //
                .rootId(shareNodeRootId) //
                .shareId(shareId) //
                .sessionId(sessionId) //
                .newNodeId(newNodeId) //
                .title(title) //
                .note(note) //
                .build();
        var result = requestProxy.sendCreateNodePushAndPoll(args);
        if (hasErrors(result)) {
            throw new RuntimeException();
        }
        return new Result(extractLastPartOfUUID(newNodeId));
    }

    public Result editNode(String nodeShortId, String title, String note) {
        workflowyStateUpdater.updateState();
        var nodeOpt = workflowyStateUpdater.findNode(nodeShortId);
        if (nodeOpt.isEmpty()) {
            return createNode(title, note, false);
        }
        var args = EditNodePushAndPollArgs.builder() //
                .transactionId(workflowyStateUpdater.getMostRecentOperationTransactionId()) //
                .dateJoinedTimestampInSeconds(dateJoinedTimestampInSeconds) //
                .nodeId(nodeOpt.orElseThrow().getId()) //
                .shareId(shareId) //
                .sessionId(sessionId) //
                .title(title) //
                .note(note) //
                .build();
        var result = requestProxy.sendEditNodePushAndPoll(args);
        if (hasErrors(result)) {
            throw new RuntimeException();
        }
        return new Result(extractLastPartOfUUID(nodeShortId));
    }

    private static String generateRandomNodeId() {
        return UUID.randomUUID().toString();
    }

    private static boolean hasErrors(Root result) {
        return result.getResults() //
                .stream() //
                .anyMatch(org.phdezann.cn.wf.json.push_and_poll.response.Result::isErrorEncounteredInRemoteOperations);
    }

    private String extractLastPartOfUUID(String bulletId) {
        if (!StringUtils.contains(bulletId, "-")) {
            return bulletId;
        }
        return substringAfterLast(bulletId, "-");
    }

}
