package org.phdezann.cn.core;

import static org.phdezann.cn.core.WorkflowyFormatter.bold;
import static org.phdezann.cn.core.WorkflowyFormatter.colored;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.Config.ConfigKey;
import org.phdezann.cn.core.WorkflowyFormatter.COLOR;
import org.phdezann.cn.core.model.Event;
import org.phdezann.cn.core.model.EventDateOrTime;

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
        var summary = event.getSummary().map(StringUtils::trimToEmpty).orElse("(vide)");
        var start = event.getStart();
        var end = event.getEnd();

        var dayStart = getDay(event.hasDates(), start);
        var dayEnd = isSameDay(event.hasDates(), start, end) ? //
                Optional.<LocalDate> empty() : Optional.of(getDay(event.hasDates(), end));
        var dayInfo = getDayInfo(dayStart, dayEnd);

        var title = String.format("%s | %s", colored(escape(summary), COLOR.GRAY), colored(dayInfo, COLOR.SKY));

        if (config.isTrue(ConfigKey.ENABLE_HOSTNAME_IN_TITLE)) {
            title += " #" + getHostname();
        }

        return title;
    }

    private String getDayInfo(LocalDate dayStart, Optional<LocalDate> dayEnd) {
        return dayEnd //
                .map(str -> String.format("%s au %s", formatDate(dayStart), formatDate(str))) //
                .orElse(formatDate(dayStart));
    }

    private String buildNote(Event event) {
        var htmlLink = String.format("<a href=\"%s\">Lien</a>", event.getHtmlLink());
        var prefixLength = NotePrefixLengthFinder.findPrefixLength(event.getSummary());
        var prefix = StringUtils.repeat(" ", prefixLength);
        if (event.hasDates()) {
            return String.format("%s%s", prefix, htmlLink);
        } else {
            var time = event.getStart().getTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            return String.format("%s%s | %s", prefix, colored(time, COLOR.SKY), htmlLink);
        }
    }

    private LocalDate getDay(boolean dateOnly, EventDateOrTime eventDateTime) {
        return dateOnly ? dateToLocalDate(eventDateTime) : timeToLocalDate(eventDateTime);
    }

    private String formatDate(LocalDate date) {
        var pattern = DateTimeFormatter.ofPattern("d LLLL yyyy", Locale.FRENCH);
        return date.format(pattern);
    }

    private boolean isSameDay(boolean dateOnly, EventDateOrTime eventStart, EventDateOrTime eventEnd) {
        if (dateOnly) {
            return dateToLocalDate(eventStart).equals(dateToLocalDate(eventEnd));
        } else {
            return timeToLocalDate(eventStart).equals(timeToLocalDate(eventEnd));
        }
    }

    private LocalDate dateToLocalDate(EventDateOrTime eventDateTime) {
        if (eventDateTime.getDate() == null) {
            throw new NullPointerException();
        }
        return eventDateTime.getDate();
    }

    private LocalDate timeToLocalDate(EventDateOrTime eventDateTime) {
        if (eventDateTime.getTime() == null) {
            throw new NullPointerException();
        }
        return eventDateTime.getTime().toLocalDate();
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String escape(String text) {
        text = StringUtils.replace(text, "&", "&amp;");
        text = StringUtils.replace(text, ">", "&gt;");
        text = StringUtils.replace(text, "<", "&lt;");
        return text;
    }

}
