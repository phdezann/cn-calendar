package org.phdezann.cn.core;

import static org.phdezann.cn.core.model.EventStatusEnum.CANCELLED;
import static org.phdezann.cn.core.model.EventStatusEnum.OTHER;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.BulletCache.CacheValue;
import org.phdezann.cn.core.Config.ConfigKey;
import org.phdezann.cn.core.EventFormatter.WorkflowyBullet;
import org.phdezann.cn.core.converter.EventConverter;
import org.phdezann.cn.core.converter.EventStatusEnumConverter;
import org.phdezann.cn.core.model.Event;
import org.phdezann.cn.support.FileUtils;
import org.phdezann.cn.wf.core.WorkflowyClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class EventCreator {

    private final AppArgs appArgs;
    private final Config config;
    private final GoogleCalendar googleCalendar;
    private final ChannelCache channelCache;
    private final EventStatusEnumConverter eventStatusEnumConverter;
    private final EventConverter eventConverter;
    private final EventFormatter eventFormatter;
    private final WorkflowyClient workflowyClient;
    private final BulletCache bulletCache;
    private final JsonSerializer jsonSerializer;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public synchronized void onNotification(String channelId) {
        log.trace("Got notification for channelId#{}", channelId);
        channelCache.getAllValues() //
                .stream() //
                .filter(cacheValue -> cacheValue.channelId().equals(channelId)) //
                .findAny() //
                .ifPresent(cacheValue -> createEvents(cacheValue.calendarId()));
    }

    public synchronized void createEventsOnStartup() {
        log.info("Synchronizing events with no notification");
        googleCalendar //
                .getCalendars(config.get(ConfigKey.CALENDAR_TITLES)) //
                .forEach(this::createEvents);
    }

    private void createEvents(String calendarId) {
        googleCalendar //
                .getEvents(calendarId) //
                .stream() //
                .map(event -> {
                    var status = eventStatusEnumConverter.convert(event.getStatus());
                    if (status == CANCELLED) {
                        return googleCalendar.getEvent(calendarId, event.getId());
                    }
                    return event;
                }) //
                .peek(this::dumpEvent) //
                .map(eventConverter::convert) //
                .forEach(this::createEvent);
    }

    private void createEvent(Event event) {
        if (event.getStatus() == OTHER) {
            log.warn("Ignoring event with status {}", event.getStatus());
            return;
        }

        log.info("Event#{} '{}' '{}' to be synced", event.getId(), event.getSummary().orElse(""), event.getStatus());
        var workflowyBullet = formatWorkflowyBullet(event);
        var bulletId = createOrUpdateBullet(event, workflowyBullet);
        log.info("Bullet#{} in sync", bulletId);
    }

    private void dumpEvent(com.google.api.services.calendar.model.Event event) {
        var eventDir = appArgs.getEventDir();
        if (eventDir == null) {
            return;
        }
        FileUtils.forceMkdir(eventDir);
        var json = jsonSerializer.writeValue(event);
        var output = new File(eventDir, UUID.randomUUID().toString());
        FileUtils.write(output, json);
        log.info("Dumped Event#{} to '{}'", event.getId(), output);
    }

    private WorkflowyBullet formatWorkflowyBullet(Event event) {
        return switch (event.getStatus()) {
        case CONFIRMED -> eventFormatter.formatConfirmed(event);
        case CANCELLED -> eventFormatter.formatCancelledEvent(event);
        default -> throw new IllegalArgumentException();
        };
    }

    private String createOrUpdateBullet(Event event, WorkflowyBullet workflowyBullet) {
        var cachedValue = bulletCache.get(event.getId());
        if (!hasChanged(workflowyBullet, cachedValue)) {
            return cachedValue.orElseThrow().bulletId();
        }
        var bulletId = workflowyClient //
                .createOrEditNode(event.getHtmlLink(), workflowyBullet.title(), workflowyBullet.note()) //
                .nodeShortId();
        updateCache(event.getId(), bulletId, workflowyBullet);
        return bulletId;
    }

    private boolean hasChanged(WorkflowyBullet workflowyBullet, Optional<CacheValue> cachedValue) {
        return cachedValue.stream() //
                .noneMatch(elt -> StringUtils.equals(elt.bulletTitle(), workflowyBullet.title()) //
                        && StringUtils.equals(elt.bulletNote(), workflowyBullet.note()));
    }

    private void updateCache(String eventId, String bulletId, WorkflowyBullet workflowyBullet) {
        bulletCache.set(new CacheValue(eventId, bulletId, workflowyBullet.title(), workflowyBullet.note()));
    }

    public void setupEventsEveryDay() {
        var now = ZonedDateTime.now();

        var todayAt0630 = now //
                .withHour(6) //
                .withMinute(30) //
                .withSecond(0) //
                .withNano(0);
        scheduleEveryDay(todayAt0630, this::createEventsOnStartup);
    }

    private void scheduleEveryDay(ZonedDateTime tick, Runnable command) {
        var now = ZonedDateTime.now();
        if (tick.isBefore(now)) {
            tick = tick.plusDays(1);
        }
        var delay = Duration.between(now, tick).getSeconds();
        var oneDayInSeconds = 60 * 60 * 24;
        scheduler.scheduleWithFixedDelay(command, delay, oneDayInSeconds, TimeUnit.SECONDS);
    }

}
