package org.phdezann.cn.core;

import static org.phdezann.cn.core.model.EventStatusEnum.CANCELLED;
import static org.phdezann.cn.core.model.EventStatusEnum.CONFIRMED;
import static org.phdezann.cn.core.model.EventStatusEnum.OTHER;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.BulletCache.CacheValue;
import org.phdezann.cn.core.EventFormatter.WorkflowyBullet;
import org.phdezann.cn.core.LinkParser.WorkflowyLink;
import org.phdezann.cn.core.converter.EventConverter;
import org.phdezann.cn.core.converter.EventStatusEnumConverter;
import org.phdezann.cn.core.model.Event;
import org.phdezann.cn.support.FileUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class EventCreator {

    private final AppArgs appArgs;
    private final GoogleCalendar googleCalendar;
    private final ChannelCache channelCache;
    private final EventStatusEnumConverter eventStatusEnumConverter;
    private final EventConverter eventConverter;
    private final EventFormatter eventFormatter;
    private final org.phdezann.cn.wf.core.WorkflowyClient workflowyClient;
    private final LinkParser linkParser;
    private final BulletCache bulletCache;
    private final DescriptionUpdater descriptionUpdater;
    private final JsonSerializer jsonSerializer;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public synchronized void onNotification(String channelId) {
        log.trace("Got notification for channelId#{}", channelId);
        channelCache.getAllValues() //
                .stream() //
                .filter(cacheValue -> cacheValue.getChannelId().equals(channelId)) //
                .findAny() //
                .ifPresent(cacheValue -> createEvents(cacheValue.getCalendarId()));
    }

    public synchronized void createEventsOnStartup() {
        log.info("Synchronizing events with no notification");
        channelCache.getAllValues() //
                .forEach(cacheValue -> createEvents(cacheValue.getCalendarId()));
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
                .forEach(event -> createEvent(calendarId, event));
    }

    private void createEvent(String calendarId, Event event) {
        if (event.getStatus() == OTHER) {
            log.warn("Ignoring event with status {}", event.getStatus());
            return;
        }

        log.info("Event#{} '{}' '{}' to be synced", event.getId(), event.getSummary().orElse(""), event.getStatus());
        var description = event.getDescription();
        var workflowyLink = linkParser.extractWorkflowyLink(description);
        var bulletIdInDesc = workflowyLink.map(WorkflowyLink::getBulletId);
        var workflowyBullet = formatWorkflowyBullet(event);
        var bulletId = createOrUpdateBullet(event, workflowyBullet, bulletIdInDesc);
        updateBulletCache(bulletId, workflowyBullet);
        var updatedDescription = descriptionUpdater.update(description, bulletId, workflowyLink);
        var descriptionHasChanged = description.stream().noneMatch(elt -> StringUtils.equals(updatedDescription, elt));
        if (event.getStatus().equals(CONFIRMED) && descriptionHasChanged) {
            googleCalendar.updateDescription(calendarId, event.getId(), updatedDescription);
            log.info("Updated description for event#{}", event.getId());
        }
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

    private String createOrUpdateBullet(Event event, WorkflowyBullet workflowyBullet, Optional<String> bulletIdInDesc) {
        var title = workflowyBullet.getTitle();
        var note = workflowyBullet.getNote();

        if (bulletIdInDesc.isEmpty() || isNewlyCreatedEvent(event)) {
            var bulletId = workflowyClient.createNode(title, note).getNodeShortId();
            log.debug("Bullet#{} created", bulletId);
            return bulletId;
        } else {
            var previousBulletId = bulletIdInDesc.orElseThrow();
            if (!titleOrNoteHasChanged(previousBulletId, title, note)) {
                return previousBulletId;
            }
            var bulletId = workflowyClient.editNode(previousBulletId, title, note).getNodeShortId();
            if (StringUtils.equals(previousBulletId, bulletId)) {
                log.debug("Bullet#{} updated", bulletId);
            } else {
                log.debug("Bullet#{} created because previous Bullet#{} was missing", bulletId, previousBulletId);
            }
            return bulletId;
        }
    }

    private void updateBulletCache(String bulletId, WorkflowyBullet workflowyBullet) {
        bulletCache.set(new CacheValue(bulletId, workflowyBullet.getTitle(), workflowyBullet.getNote()));
    }

    private boolean titleOrNoteHasChanged(String bulletId, String title, String note) {
        return bulletCache.get(bulletId) //
                .stream() //
                .noneMatch(cachedValue -> StringUtils.equals(cachedValue.getBulletTitle(), title) //
                        && StringUtils.equals(cachedValue.getBulletNote(), note));
    }

    private boolean isNewlyCreatedEvent(Event event) {
        var created = event.getCreated();
        var updated = event.getUpdated();
        var diffInSeconds = ChronoUnit.SECONDS.between(created, updated);
        return diffInSeconds == 0;
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
        scheduler //
                .scheduleWithFixedDelay(command, delay, oneDayInSeconds, TimeUnit.SECONDS);
    }

}
