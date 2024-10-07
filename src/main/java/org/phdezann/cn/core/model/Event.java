package org.phdezann.cn.core.model;

import java.time.ZonedDateTime;
import java.util.Optional;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
@Getter
public class Event {
    private final String id;
    private final EventStatusEnum status;
    private final Optional<String> summary;
    private final Optional<String> description;
    private final String htmlLink;
    private final Optional<ZonedDateTime> created;
    private final ZonedDateTime updated;
    private final EventDateOrTime start;
    private final EventDateOrTime end;

    public boolean hasDates() {
        return start.isDateValue() && end.isDateValue();
    }
}
