package org.phdezann.cn.core;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.remove;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.Optional;

import org.phdezann.cn.core.LinkParser.WorkflowyLink;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DescriptionUpdater {

    private final LinkParser linkParser;

    public String update(Optional<String> descriptionOpt, String bulletId, Optional<WorkflowyLink> link) {
        var description = trimToEmpty(extractRawDescription(descriptionOpt, link));
        var separator = isEmpty(description) ? "" : "\n";
        return description + separator + linkParser.buildLink(bulletId);
    }

    private String extractRawDescription(Optional<String> descriptionOpt, Optional<WorkflowyLink> linkOpt) {
        return descriptionOpt //
                .map(description -> linkOpt.map(link -> remove(description, link.getWholeLink())).orElse(description))
                .orElse("");
    }
}
