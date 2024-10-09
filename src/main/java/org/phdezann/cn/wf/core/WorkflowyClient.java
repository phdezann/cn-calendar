package org.phdezann.cn.wf.core;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.Config;
import org.phdezann.cn.core.Config.ConfigKey;
import org.phdezann.cn.wf.core.RequestProxy.CreateNodePushAndPollArgs;
import org.phdezann.cn.wf.core.RequestProxy.EditNodePushAndPollArgs;
import org.phdezann.cn.wf.json.push_and_poll.response.Root;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    private void open(String shareSecretLink) {
        var response = requestProxy.sendShareSecretLink(shareSecretLink);
        sessionId = response.sessionId();
        shareId = response.shareId();
        var initializationData = requestProxy.sendGetInitializationData(sessionId, shareId);
        dateJoinedTimestampInSeconds = //
                initializationData //
                        .getProjectTreeData() //
                        .getAuxiliaryProjectTreeInfos() //
                        .getFirst() //
                        .getDateJoinedTimestampInSeconds();
        workflowyStateUpdater = new WorkflowyStateUpdater(requestProxy, shareId, sessionId);
        shareNodeRootId = workflowyStateUpdater.getNodeRootId();
    }

    public record Result(String nodeShortId) {
        public Result {
            nodeShortId = extractLastPartOfUUID(nodeShortId);
        }

        private String extractLastPartOfUUID(String bulletId) {
            if (!StringUtils.contains(bulletId, "-")) {
                return bulletId;
            }
            return substringAfterLast(bulletId, "-");
        }
    }

    public Result createOrEditNode(String noteFragment, String title, String note) {
        workflowyStateUpdater.updateState();
        var nodeOpt = workflowyStateUpdater.findNodeByNoteFragment(noteFragment);
        if (nodeOpt.isEmpty()) {
            return createNode(title, note);
        }
        var node = nodeOpt.orElseThrow();
        if (StringUtils.equals(node.getNm(), title) && StringUtils.equals(node.getNo(), note)) {
            log.debug("No modification detected for node#{}", node.getId());
            return new Result(node.getId());
        }
        return editNode(node.getId(), title, note);
    }

    private Result createNode(String title, String note) {
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
        return new Result(newNodeId);
    }

    private Result editNode(String nodeId, String title, String note) {
        var args = EditNodePushAndPollArgs.builder() //
                .transactionId(workflowyStateUpdater.getMostRecentOperationTransactionId()) //
                .dateJoinedTimestampInSeconds(dateJoinedTimestampInSeconds) //
                .nodeId(nodeId) //
                .shareId(shareId) //
                .sessionId(sessionId) //
                .title(title) //
                .note(note) //
                .build();
        var result = requestProxy.sendEditNodePushAndPoll(args);
        if (hasErrors(result)) {
            throw new RuntimeException();
        }
        return new Result(nodeId);
    }

    private static String generateRandomNodeId() {
        return UUID.randomUUID().toString();
    }

    private static boolean hasErrors(Root result) {
        return result.getResults() //
                .stream() //
                .anyMatch(org.phdezann.cn.wf.json.push_and_poll.response.Result::isErrorEncounteredInRemoteOperations);
    }

}
