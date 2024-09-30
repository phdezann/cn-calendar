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
public class DataRequest {
    @JsonProperty("parentid")
    private String parentId;
    @JsonProperty("starting_priority")
    private long startingPriority;
    @JsonProperty("project_trees")
    private String projectTrees;
    @JsonProperty("isForSearch")
    private boolean forSearch;
}

