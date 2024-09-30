package org.phdezann.cn.wf.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class InitialHtmlPageUtilsTest {

    @Test
    void extractSessionId() {
        assertThat(InitialHtmlPageUtils.extractSessionId(List.of("name=value,", "sessionid=123;"))) //
                .isEqualTo("123");
    }

    @Test
    void findShareId() {
        assertThat(InitialHtmlPageUtils.findShareId("var PROJECT_TREE_DATA_URL_PARAMS = {\"share_id\": \"123\"}")) //
                .isEqualTo("123");
    }
}
