package org.phdezann.cn.wf.core;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InitialHtmlPageUtils {

    public static String extractSessionId(List<String> cookies) {
        return cookies.stream() //
                .map(InitialHtmlPageUtils::findSessionId) //
                .filter(Optional::isPresent) //
                .map(Optional::get) //
                .findFirst() //
                .orElseThrow();
    }

    public static String findShareId(String text) {
        var regex = "var PROJECT_TREE_DATA_URL_PARAMS = \\{\"share_id\": \"(.*?)\"}";
        Pattern p = Pattern.compile(regex, Pattern.DOTALL);
        Matcher m = p.matcher(text);
        boolean matches = m.find();
        if (!matches) {
            throw new IllegalArgumentException();
        }
        return m.group(1);
    }

    private static Optional<String> findSessionId(String text) {
        var regex = "sessionid=(.*?);";
        var p = Pattern.compile(regex, Pattern.DOTALL);
        var m = p.matcher(text);
        boolean matches = m.find();
        if (!matches) {
            return Optional.empty();
        }
        return Optional.of(m.group(1));
    }

}
