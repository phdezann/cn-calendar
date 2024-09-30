package org.phdezann.cn.wf.core;

import java.util.Optional;

import org.phdezann.cn.wf.json.get_tree_data.Item;

public class WorkflowyStateUpdater {

    private final RequestProxy requestProxy;
    private final String shareId;
    private final String sessionId;
    private final WorkflowyState workflowyState;

    public WorkflowyStateUpdater(RequestProxy requestProxy, String shareId, String sessionId) {
        this.requestProxy = requestProxy;
        this.workflowyState = new WorkflowyState();
        this.shareId = shareId;
        this.sessionId = sessionId;
        updateState();
    }

    public void updateState() {
        var localTreeData = requestProxy.sendGetTreeData(sessionId, shareId);
        workflowyState.setLocalTreeData(localTreeData);
    }

    public String getMostRecentOperationTransactionId() {
        return workflowyState.getLocalTreeData().getMostRecentOperationTransactionId();
    }

    public String getNodeRootId() {
        return workflowyState.getLocalTreeData().getItems() //
                .stream() //
                .filter(item -> item.getPrnt() == null) //
                .findFirst() //
                .orElseThrow() //
                .getId();
    }

    public Optional<Item> findNode(String nodeShortId) {
        return workflowyState.getLocalTreeData().getItems() //
                .stream() //
                .filter(item -> item.getId().endsWith(nodeShortId)) //
                .findFirst();
    }

}
