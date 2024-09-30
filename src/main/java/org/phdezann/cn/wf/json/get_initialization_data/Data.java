package org.phdezann.cn.wf.json.get_initialization_data;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Data {
    private List<AuxiliaryProject> auxiliaryProjectTreeInfos = new ArrayList<>();
}
