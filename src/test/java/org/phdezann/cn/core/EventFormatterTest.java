package org.phdezann.cn.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.phdezann.cn.core.WorkflowyFormatter.colored;
import static org.phdezann.cn.core.WorkflowyFormatter.toHref;

import org.junit.jupiter.api.Test;
import org.phdezann.cn.core.WorkflowyFormatter.COLOR;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

class EventFormatterTest {

    final EventFormatter eventFormatter = new EventFormatter(new Config());

    @Test
    void format_span_2_days_with_time() {
        var event = build("RDV chez Dr Valentin Chiruc", //
                "2024-04-15T13:10:00+02:00", //
                "2024-04-17T13:10:00+02:00", //
                "https://www.google.com/calendar/event?eid=Mzlk");

        var bullet = eventFormatter.formatConfirmed(event);

        assertThat(bullet.getTitle()) //
                .startsWith("" //
                        + colored("RDV chez Dr Valentin Chiruc", COLOR.GRAY) //
                        + " | " //
                        + colored("15 avril 2024 au 17 avril 2024", COLOR.SKY));
        assertThat(bullet.getNote()) //
                .isEqualTo("" //
                        + colored("13:10", COLOR.SKY) //
                        + " | " //
                        + toHref("https://www.google.com/calendar/event?eid=Mzlk", "Lien"));
    }

    @Test
    void format_span_2_days_no_time() {
        var event = build("RDV chez Dr Valentin Chiruc", //
                "2024-04-15", //
                "2024-04-17", //
                "https://www.google.com/calendar/event?eid=Mzlk");

        var bullet = eventFormatter.formatConfirmed(event);

        assertThat(bullet.getTitle()) //
                .startsWith("" //
                        + colored("RDV chez Dr Valentin Chiruc", COLOR.GRAY) //
                        + " | " //
                        + colored("15 avril 2024 au 17 avril 2024", COLOR.SKY));
        assertThat(bullet.getNote()) //
                .isEqualTo("" //
                        + toHref("https://www.google.com/calendar/event?eid=Mzlk", "Lien"));
    }

    @Test
    void format_same_day_with_time() {
        var event = build("RDV chez Dr Valentin Chiruc", //
                "2024-04-15T13:10:00+02:00", //
                "2024-04-15T13:30:00+02:00", //
                "https://www.google.com/calendar/event?eid=Mzlk");

        var bullet = eventFormatter.formatConfirmed(event);

        assertThat(bullet.getTitle()) //
                .startsWith("" //
                            + colored("RDV chez Dr Valentin Chiruc", COLOR.GRAY) //
                            + " | " //
                            + colored("15 avril 2024", COLOR.SKY));
        assertThat(bullet.getNote()) //
                .isEqualTo("" //
                           + colored("13:10", COLOR.SKY) //
                           + " | " //
                           + toHref("https://www.google.com/calendar/event?eid=Mzlk", "Lien"));
    }

    @Test
    void format_whole_day() {
        var event = build("RDV chez Dr Valentin Chiruc", //
                "2024-04-15", //
                "2024-04-16", // for Google Calendar, for a whole day event, the end date is the day after
                "https://www.google.com/calendar/event?eid=Mzlk");

        var bullet = eventFormatter.formatConfirmed(event);

        assertThat(bullet.getTitle()) //
                .startsWith("" //
                            + colored("RDV chez Dr Valentin Chiruc", COLOR.GRAY) //
                            + " | " //
                            + colored("15 avril 2024", COLOR.SKY));
        assertThat(bullet.getNote()) //
                .isEqualTo("" //
                           + toHref("https://www.google.com/calendar/event?eid=Mzlk", "Lien"));
    }

    private Event build(String summary, String start_str, String end_str, String htmlLink) {
        var event = new Event();
        event.setSummary(summary);
        event.setStart(getEventDateTime(start_str));
        event.setEnd(getEventDateTime(end_str));
        event.setHtmlLink(htmlLink);
        return event;
    }

    private EventDateTime getEventDateTime(String date_str) {
        var dateTime = DateTime.parseRfc3339(date_str);
        var eventDateTime = new EventDateTime();
        if (dateTime.isDateOnly()) {
            eventDateTime.setDate(dateTime);
        } else {
            eventDateTime.setDateTime(dateTime);
        }
        return eventDateTime;
    }

}
