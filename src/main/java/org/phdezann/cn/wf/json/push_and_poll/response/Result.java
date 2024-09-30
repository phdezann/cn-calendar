package org.phdezann.cn.wf.json.push_and_poll.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Result {

    @JsonProperty("new_most_recent_operation_transaction_id")
    private String newMostRecentOperationTransactionId;
    @JsonProperty("error_encountered_in_remote_operations")
    private boolean errorEncounteredInRemoteOperations;

}
