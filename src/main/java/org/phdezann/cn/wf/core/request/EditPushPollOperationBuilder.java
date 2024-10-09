package org.phdezann.cn.wf.core.request;

import org.phdezann.cn.wf.json.push_and_poll.request.edit.DataRequest;
import org.phdezann.cn.wf.json.push_and_poll.request.edit.Operation;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EditPushPollOperationBuilder {

    @Builder
    public record EditPushPollOperationArgs(String nodeId, //
            String shareId, //
            String transactionId, //
            Long clientTimeStamp, //
            String title, //
            String note) {
    }

    public Operation build(EditPushPollOperationArgs args) {
        var operation = new org.phdezann.cn.wf.json.push_and_poll.request.edit.Operation();
        operation.setType("edit");
        operation.setData(buildDataRequest(args));
        operation.setClientTimestamp(args.clientTimeStamp());
        operation.setExecutedBy(-1);
        return operation;
    }

    private DataRequest buildDataRequest(EditPushPollOperationArgs args) {
        var data = new DataRequest();
        data.setProjectId(args.nodeId());
        data.setName(args.title());
        data.setDescription(args.note());
        return data;
    }

}
