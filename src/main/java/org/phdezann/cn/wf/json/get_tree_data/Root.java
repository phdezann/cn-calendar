package org.phdezann.cn.wf.json.get_tree_data;

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
    private List<Item> items = new ArrayList<>();
    @JsonProperty("most_recent_operation_transaction_id")
    private String mostRecentOperationTransactionId;
}
