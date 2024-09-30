package org.phdezann.cn.wf.json.get_tree_data;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Item {
    private String id;
    private String nm;
    private long ct;
    private long lm;
    private String prnt;
}
