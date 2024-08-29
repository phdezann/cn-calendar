package org.phdezann.cn.core;

import static java.lang.Boolean.TRUE;
import static org.phdezann.cn.core.WorkflowyFormatter.bold;
import static org.phdezann.cn.core.WorkflowyFormatter.colored;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.Config.ConfigKey;
import org.phdezann.cn.core.WorkflowyFormatter.COLOR;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventFormatter {

    private final Config config;

    @RequiredArgsConstructor
    @Getter
    public static class WorkflowyBullet {
        private final String title;
        private final String note;

    }

    public WorkflowyBullet formatConfirmed(Event event) {
        var title = buildTitle(event);
        var note = buildNote(event);
        return new WorkflowyBullet(title, note);
    }

    public WorkflowyBullet formatCancelledEvent(Event event) {
        var bullet = formatConfirmed(event);
        var title = bullet.getTitle();
        var note = bullet.getNote();
        title = String.format("%s %s", bold(colored("[CANCELLED]", COLOR.GRAY)), title);
        return new WorkflowyBullet(title, note);
    }

    private String buildTitle(Event event) {
        var summary = event.getSummary();
        var start = event.getStart();
        var end = event.getEnd();

        var dayStart = getDay(start);
        var dayEnd = isSameDay(start, end) ? Optional.<String> empty() : Optional.of(getDay(end));
        var hostname = "#" + getHostname();
        var dayInfo = getDayInfo(dayStart, dayEnd);

        var title = String.format("%s | %s", colored(summary, COLOR.GRAY), colored(dayInfo, COLOR.SKY));

        if (config.get(ConfigKey.DISABLE_HOSTNAME_IN_TITLE).equalsIgnoreCase("true")) {
            title += " " + hostname;
        }

        return title;
    }

    private String getDayInfo(String dayStart, Optional<String> dayEnd) {
        if (dayEnd.isEmpty()) {
            return dayStart;
        }
        return String.format("%s au %s", dayStart, dayEnd.get());
    }

    private String buildNote(Event event) {
        var start = event.getStart();
        var end = event.getEnd();
        var htmlLink = String.format("<a href=\"%s\">Lien</a>", event.getHtmlLink());
        var prefixLength = NotePrefixLengthFinder.findPrefixLength(event.getSummary());
        var prefix = StringUtils.repeat(" ", prefixLength);
        if (isDateOnly(start) && isDateOnly(end)) {
            return String.format("%s%s", prefix, htmlLink);
        } else {
            var time = toZonedDateTime(start).format(DateTimeFormatter.ofPattern("HH:mm"));
            return String.format("%s%s | %s", prefix, colored(time, COLOR.SKY), htmlLink);
        }
    }

    private String getDay(EventDateTime eventDateTime) {
        var pattern = DateTimeFormatter.ofPattern("d LLLL yyyy", Locale.FRENCH);
        return toLocalDate(eventDateTime).format(pattern);
    }

    private boolean isDateOnly(EventDateTime eventDateTime) {
        return eventDateTime.getDate() != null && eventDateTime.getDateTime() == null;
    }

    private boolean isSameDay(EventDateTime eventStart, EventDateTime eventEnd) {
        if (isDateOnly(eventStart) && isDateOnly(eventEnd)) {
            return toLocalDate(eventStart).plusDays(1).equals(toLocalDate(eventEnd));
        } else {
            return toZonedDateTime(eventStart).toLocalDate().equals(toZonedDateTime(eventEnd).toLocalDate());
        }
    }

    private LocalDate toLocalDate(EventDateTime eventDateTime) {
        if (isDateOnly(eventDateTime)) {
            return DateTimeConverter.toLocalDate(eventDateTime.getDate());
        } else {
            return DateTimeConverter.toZonedDateTime(eventDateTime.getDateTime()).toLocalDate();
        }
    }

    private ZonedDateTime toZonedDateTime(EventDateTime eventDateTime) {
        if (isDateOnly(eventDateTime)) {
            throw new RuntimeException("Must be a datetime");
        }
        return DateTimeConverter.toZonedDateTime(eventDateTime.getDateTime());
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

}
