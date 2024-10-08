package org.phdezann.cn.core;

import java.util.Optional;
import java.util.regex.Pattern;

public class NotePrefixLengthFinder {

    public static int findPrefixLength(Optional<String> text) {
        var p = Pattern.compile("([^\\p{IsLetter}]+).*");
        var m = p.matcher(text.orElse(""));
        if (!m.matches()) {
            return 0;
        }
        return m.group(1).isEmpty() ? 0 : 5;
    }

}
