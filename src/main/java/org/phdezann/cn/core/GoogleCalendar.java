package org.phdezann.cn.core;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.phdezann.cn.core.Config.ConfigKey;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Channel;
import com.google.api.services.calendar.model.Event;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GoogleCalendar {

    private final Config config;
    private final GoogleClient googleClient;
    private final Calendar calendar;
    private final SyncTokenCache syncTokenCache;

    public GoogleCalendar(Config config, GoogleClient googleClient, SyncTokenCache syncTokenCache) {
        this.config = config;
        this.googleClient = googleClient;
        this.calendar = getService();
        this.syncTokenCache = syncTokenCache;
    }

    public List<String> getCalendars(String titles) {
        var listOfTitle = Arrays //
                .stream(titles.split(",")) //
                .map(StringUtils::trim) //
                .toList();
        try {
            return calendar //
                    .calendarList() //
                    .list() //
                    .execute() //
                    .getItems() //
                    .stream() //
                    .filter(item -> listOfTitle.contains(item.getSummary())) //
                    .map(CalendarListEntry::getId) //
                    .toList();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Event> getEvents(String calendarId) {
        try {
            var request = calendar.events().list(calendarId);
            var syncToken = syncTokenCache.get(calendarId);
            if (syncToken.isEmpty()) {
                var nowMinus1Min = new DateTime(ZonedDateTime //
                        .now() //
                        .minusMinutes(1) //
                        .withZoneSameInstant(ZoneId.of("UTC")) //
                        .toEpochSecond() * 1000);
                log.debug("Getting all events from {}", nowMinus1Min);
                request.setUpdatedMin(nowMinus1Min);
            } else {
                log.debug("Using previously received 'SyncToken' for calendar#{}", calendarId);
                request.setSyncToken(syncToken.orElseThrow());
            }
            var events = request.execute();
            syncTokenCache.set(calendarId, events.getNextSyncToken());
            return events.getItems();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Event getEvent(String calendarId, String eventId) {
        try {
            return calendar.events().get(calendarId, eventId).execute();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Calendar getService() {
        var credentials = googleClient.getCredentials();

        return new Calendar //
                .Builder(credentials.getTransport(), credentials.getJsonFactory(), credentials) //
                .setApplicationName(GoogleClient.APPLICATION_NAME) //
                .build();
    }

    public WatchResponse watch(String calendarId, ZonedDateTime expiration, String token) {
        try {
            return doWatch(calendarId, expiration, token);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void stopWatchIgnoreNotFoundError(String channelId, String resourceId) {
        try {
            doStopWatch(channelId, resourceId);
        } catch (IOException ex) {
            if (ex instanceof GoogleJsonResponseException json //
                    && json.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                log.info("Channel#{} not found, could not be stopped", channelId, ex);
                return;
            }
            throw new RuntimeException(ex);
        }
    }

    public record WatchResponse(String channelId, String resourceId, ZonedDateTime expiration) {
    }

    private WatchResponse doWatch(String calendarId, ZonedDateTime expiration, String token) throws IOException {
        var channel = new Channel();
        channel.setAddress(config.get(ConfigKey.CALENDAR_WEBHOOK));
        channel.setType("web_hook");
        channel.setExpiration(expiration.toInstant().toEpochMilli());
        channel.setId(UUID.randomUUID().toString());
        channel.setToken(token);

        var result = calendar.events().watch(calendarId, channel).execute();

        var utc = ZoneId.of("UTC");
        var expirationInResponse = LocalDateTime //
                .ofInstant(Instant.ofEpochMilli(result.getExpiration()), utc).atZone(utc) //
                .withZoneSameInstant(ZoneId.of("Europe/Paris"));

        return new WatchResponse(result.getId(), result.getResourceId(), expirationInResponse);
    }

    private void doStopWatch(String channelId, String resourceId) throws IOException {
        var channel = new Channel();
        channel.setId(channelId);
        channel.setResourceId(resourceId);

        calendar.channels().stop(channel).execute();
        log.debug("Stopped channel#{}", channelId);
    }

}
