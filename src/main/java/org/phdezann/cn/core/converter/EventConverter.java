package org.phdezann.cn.core.converter;

import static org.phdezann.cn.core.DateTimeConverter.toZonedDateTime;

import org.phdezann.cn.core.model.Event;
import org.phdezann.cn.core.model.EventDateOrTime;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventConverter {

    private final EventStatusEnumConverter eventStatusConverter;
    private final EventDateTimeConverter eventDateTimeConverter;

    public Event convert(com.google.api.services.calendar.model.Event src) {
        var status = eventStatusConverter.convert(src.getStatus());
        var summary = Optional.ofNullable(src.getSummary());
        var description = Optional.ofNullable(src.getDescription());
        var start = eventDateTimeConverter.convert(src.getStart());
        var end = fixEndDate(eventDateTimeConverter.convert(src.getEnd()));
        var created = toZonedDateTime(src.getCreated());
        var updated = toZonedDateTime(src.getUpdated());
        return Event.builder() //
                .id(src.getId()) //
                .status(status) //
                .summary(summary) //
                .description(description) //
                .htmlLink(src.getHtmlLink()) //
                .created(created) //
                .updated(updated) //
                .start(start) //
                .end(end) //
                .build();
    }

    private EventDateOrTime fixEndDate(EventDateOrTime eventDateOrTime) {
        if (eventDateOrTime.isDateValue()) {
            return new EventDateOrTime(true, eventDateOrTime.getDate().minusDays(1), null);
        }
        return eventDateOrTime;
    }

}
