package org.phdezann.cn.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.phdezann.cn.core.WorkflowyFormatter.colored;
import static org.phdezann.cn.core.WorkflowyFormatter.toHref;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.phdezann.cn.core.WorkflowyFormatter.COLOR;
import org.phdezann.cn.core.converter.EventDateTimeConverter;
import org.phdezann.cn.core.model.Event;
import org.phdezann.cn.core.model.EventDateOrTime;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.EventDateTime;

class EventFormatterTest {

    private final EventFormatter eventFormatter = new EventFormatter(new Config());
    private final EventDateTimeConverter eventDateTimeConverter = new EventDateTimeConverter();

    @Test
    void format_span_2_days_with_time() {
        var event = build(Optional.of("RDV chez Dr Valentin Chiruc"), //
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
        var event = build(Optional.of("RDV chez Dr Valentin Chiruc"), //
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
        var event = build(Optional.of("RDV chez Dr Valentin Chiruc"), //
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
        var event = build(Optional.of("RDV chez Dr Valentin Chiruc"), //
                "2024-04-15", //
                "2024-04-15", //
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

    @Test
    void format_empty_summary() {
        var event = build(Optional.empty(), //
                "2024-04-15", //
                "2024-04-15", //
                "https://www.google.com/calendar/event?eid=Mzlk");

        var bullet = eventFormatter.formatConfirmed(event);

        assertThat(bullet.getTitle()) //
                .startsWith("" //
                        + colored("(vide)", COLOR.GRAY) //
                        + " | " //
                        + colored("15 avril 2024", COLOR.SKY));
        assertThat(bullet.getNote()) //
                .isEqualTo("" //
                        + toHref("https://www.google.com/calendar/event?eid=Mzlk", "Lien"));
    }

    @Test
    void escape_html_entities_in_summary() {
        var event = build(Optional.of("My event &"), //
                "2024-04-15", //
                "2024-04-15", //
                "https://www.google.com/calendar/event?eid=Mzlk");

        var bullet = eventFormatter.formatConfirmed(event);

        assertThat(bullet.getTitle()) //
                .startsWith("" //
                            + colored("My event &amp;", COLOR.GRAY) //
                            + " | " //
                            + colored("15 avril 2024", COLOR.SKY));
    }

    @Test
    void trim_summary() {
        var event = build(Optional.of("  My event "), //
                "2024-04-15", //
                "2024-04-15", //
                "https://www.google.com/calendar/event?eid=Mzlk");

        var bullet = eventFormatter.formatConfirmed(event);

        assertThat(bullet.getTitle()) //
                .startsWith("" //
                            + colored("My event", COLOR.GRAY) //
                            + " | " //
                            + colored("15 avril 2024", COLOR.SKY));
    }



    private Event build(Optional<String> summary, String start_str, String end_str, String htmlLink) {
        return Event.builder() //
                .summary(summary) //
                .start(getEventDateTime(start_str)) //
                .end(getEventDateTime(end_str)) //
                .htmlLink(htmlLink).build();
    }

    private EventDateOrTime getEventDateTime(String date_str) {
        var dateTime = DateTime.parseRfc3339(date_str);
        var isDateOnly = dateTime.isDateOnly();
        var eventDateTime = new EventDateTime();
        if (isDateOnly) {
            eventDateTime.setDate(dateTime);
        } else {
            eventDateTime.setDateTime(dateTime);
        }
        return eventDateTimeConverter.convert(eventDateTime);
    }

}
