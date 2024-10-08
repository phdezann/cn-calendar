package org.phdezann.cn.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class NotePrefixLengthFinderTest {

    @Test
    void findPrefixLength() {
        assertThat(NotePrefixLengthFinder.findPrefixLength(Optional.of("🥼abc"))).isEqualTo(5);
    }

    @Test
    void findPrefixLength_accent() {
        assertThat(NotePrefixLengthFinder.findPrefixLength(Optional.of("àbc"))).isEqualTo(0);
    }

    @Test
    void findPrefixLength_noEmoji() {
        assertThat(NotePrefixLengthFinder.findPrefixLength(Optional.of("abc"))).isEqualTo(0);
    }

    @Test
    void findPrefixLength_emojiOnly() {
        assertThat(NotePrefixLengthFinder.findPrefixLength(Optional.of("🥼"))).isEqualTo(5);
    }

    @Test
    void findPrefixLength_textOnly() {
        assertThat(NotePrefixLengthFinder.findPrefixLength(Optional.of("abc"))).isEqualTo(0);
    }

    @Test
    void findPrefixLength_null() {
        assertThat(NotePrefixLengthFinder.findPrefixLength(Optional.empty())).isEqualTo(0);
    }

}
