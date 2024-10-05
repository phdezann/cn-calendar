package org.phdezann.cn.core;

public class WorkflowyFormatter {

    public enum COLOR {
        GRAY,
        SKY
    }

    public static String bold(String text) {
        return String.format("<b>%s</b>", text);
    }

    public static String colored(String text, COLOR color) {
        return String.format("<span class=\"colored c-%s\">%s</span>", color.name().toLowerCase(), text);
    }

    public static String toHref(String target, String link) {
        return String.format("<a href=\"%s\">%s</a>", target, link);
    }

}
