package org.phdezann.cn.wf.json.push_and_poll.request.edit;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Root {
    @JsonProperty("most_recent_operation_transaction_id")
    private String mostRecentOperationTransactionId;
    private List<Operation> operations = new ArrayList<>();
    @JsonProperty("share_id")
    private String shareId;
}
