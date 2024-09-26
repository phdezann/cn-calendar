package org.phdezann.cn.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class LinkParserTest {

    private final LinkParser linkParser = new LinkParser();

    @Test
    void extractWorkflowyLink() {
        var createdLink = linkParser.buildLink("c11d6723cc87");
        var extractedLink = linkParser.extractWorkflowyLink(Optional.of("ABC" + createdLink + "DEF"));

        assertThat(extractedLink).hasValueSatisfying(value -> //
        assertThat(value.getBulletId()).isEqualTo("c11d6723cc87"));
    }

}
