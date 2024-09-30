package org.phdezann.cn.wf.json.push_and_poll.request.edit;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DataRequest {
    @JsonProperty("projectid")
    private String projectId;
    private String name;
    private String description;
}
