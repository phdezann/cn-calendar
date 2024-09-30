package org.phdezann.cn.wf.json.push_and_poll.request.create;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Operation implements org.phdezann.cn.wf.json.push_and_poll.request.Operation {
    private String type;
    private DataRequest data;
    @JsonProperty("client_timestamp")
    private long clientTimestamp;
    @JsonProperty("executed_by")
    private long executedBy = -1;
}
