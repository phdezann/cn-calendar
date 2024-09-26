package org.phdezann.cn.core.converter;

import static org.phdezann.cn.core.DateTimeConverter.toZonedDateTime;

import org.phdezann.cn.core.DateTimeConverter;
import org.phdezann.cn.core.model.EventDateOrTime;

import com.google.api.services.calendar.model.EventDateTime;

public class EventDateTimeConverter {

    public EventDateOrTime convert(EventDateTime eventDateTime) {
        if (isDateOnly(eventDateTime)) {
            return new EventDateOrTime(true, DateTimeConverter.toLocalDate(eventDateTime.getDate()), null);
        } else {
            return new EventDateOrTime(false, null, toZonedDateTime(eventDateTime.getDateTime()));
        }
    }

    private boolean isDateOnly(EventDateTime eventDateTime) {
        return eventDateTime.getDate() != null && eventDateTime.getDateTime() == null;
    }

}
