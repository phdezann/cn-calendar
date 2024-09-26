package org.phdezann.cn.core;

import java.util.Optional;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

public class LinkParser {

    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class WorkflowyLink {
        private final String wholeLink;
        private final String bulletId;
    }

    public Optional<WorkflowyLink> extractWorkflowyLink(Optional<String> description) {
        if (description.isEmpty()) {
            return Optional.empty();
        }
        var pattern = Pattern.compile("(\\[workflowy:(\\S*)])");
        var matcher = pattern.matcher(description.orElseThrow());
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new WorkflowyLink(matcher.group(1), matcher.group(2)));
    }

    public String buildLink(String bulletId) {
        return String.format("[workflowy:%s]", bulletId);
    }

}
