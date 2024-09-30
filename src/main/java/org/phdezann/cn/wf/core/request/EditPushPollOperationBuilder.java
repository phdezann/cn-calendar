package org.phdezann.cn.wf.core.request;

import org.phdezann.cn.wf.json.push_and_poll.request.edit.DataRequest;
import org.phdezann.cn.wf.json.push_and_poll.request.edit.Operation;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EditPushPollOperationBuilder {

    @RequiredArgsConstructor
    @Builder
    @Getter
    public static class EditPushPollOperationArgs {
        @lombok.NonNull
        private final String nodeId;
        @lombok.NonNull
        private final String shareId;
        @lombok.NonNull
        private final String transactionId;
        @lombok.NonNull
        private final Long clientTimeStamp;
        @lombok.NonNull
        private final String title;
        @lombok.NonNull
        private final String note;
    }

    public Operation build(EditPushPollOperationArgs args) {
        var operation = new org.phdezann.cn.wf.json.push_and_poll.request.edit.Operation();
        operation.setType("edit");
        operation.setData(buildDataRequest(args));
        operation.setClientTimestamp(args.getClientTimeStamp());
        operation.setExecutedBy(-1);
        return operation;
    }

    private DataRequest buildDataRequest(EditPushPollOperationArgs args) {
        var data = new DataRequest();
        data.setProjectId(args.getNodeId());
        data.setName(args.getTitle());
        data.setDescription(args.getNote());
        return data;
    }

}
