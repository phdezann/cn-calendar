package org.phdezann.cn.wf.json.push_and_poll.request.create;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProjectTree {
    private String id;
    private long ct;
    private long cb;
}

