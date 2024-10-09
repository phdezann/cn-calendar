package org.phdezann.cn.core;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.Config.ConfigKey;
import org.phdezann.cn.support.IOUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class PushNotificationRenewer {

    public static final int CHANNEL_DURATION_IN_MINS = 60 * 24;
    public static final int MINS_BEFORE_RENEWAL = 5;
    public static final int CHECK_EVERY_MINS = 1;

    private final Config config;
    private final GoogleCalendar googleCalendar;
    private final ChannelCache channelCache;
    private final ScheduledExecutorService scheduler;
    private final ChannelLog channelLog;

    public PushNotificationRenewer(Config config, GoogleCalendar googleCalendar, ChannelCache channelCache,
            ChannelLog channelLog) {
        this.config = config;
        this.googleCalendar = googleCalendar;
        this.channelCache = channelCache;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.channelLog = channelLog;
    }

    public void startScheduler() {
        scheduler.scheduleAtFixedRate(this::renewIfExpired, 0, CHECK_EVERY_MINS, TimeUnit.MINUTES);
    }

    public void clearAll() {
        channelLog.getAllValues() //
                .forEach(c -> googleCalendar //
                        .stopWatchIgnoreNotFoundError(c.channelId(), c.resourceId()));
        channelCache.clear();
    }

    private void renewIfExpired() {
        if (!IOUtils.isUrlReachable("https://oauth2.googleapis.com")) {
            log.debug("Cannot reach mandatory server for accessing Google's API, skipping");
            return;
        }
        try {
            googleCalendar //
                    .getCalendars(config.get(ConfigKey.CALENDAR_TITLES)) //
                    .forEach(this::renewIfExpired);
        } catch (Exception ex) {
            log.error("Got error in scheduler", ex);
        }
    }

    private void renewIfExpired(String calendarId) {
        log.info("Initiating watch renew (if expired) for calendar#{}", calendarId);
        var channelOpt = channelCache //
                .getAllValues() //
                .stream() //
                .filter(value -> StringUtils.equals(value.calendarId(), calendarId)) //
                .findAny();

        if (channelOpt.isEmpty()) {
            startWatch(calendarId);
        } else {
            var channel = channelOpt.get();
            var now = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
            var expiration = channel.expiration();
            var diffInMinutes = ChronoUnit.MINUTES.between(now, expiration);
            log.info("Diff in minutes between '{}' and '{}': {}", now, expiration, diffInMinutes);
            if (diffInMinutes < MINS_BEFORE_RENEWAL) {
                log.info("Channel for calendar#{} is about to expire, renewing it before expiration on {}", calendarId,
                        expiration.toString());
                googleCalendar.stopWatchIgnoreNotFoundError(channel.channelId(), channel.resourceId());
                startWatch(calendarId);
            }
        }
    }

    private void startWatch(String calendarId) {
        log.debug("Calling watch for calendar#{}", calendarId);
        var expirationArg = ZonedDateTime.now().plusMinutes(CHANNEL_DURATION_IN_MINS);
        var token = config.get(ConfigKey.CALENDAR_WEBHOOK_TOKEN);
        var response = googleCalendar.watch(calendarId, expirationArg, token);
        log.info("Got watch response for calendar#{} with channelID:{}, expiration:{}", //
                calendarId, response.channelId(), response.expiration());
        var resourceId = response.resourceId();
        var channelId = response.channelId();
        var expiration = response.expiration();
        channelCache.set(new ChannelCache.CacheValue(calendarId, //
                resourceId, //
                channelId, //
                expiration));
        channelLog.set(new ChannelLog.CacheValue(calendarId, //
                resourceId, //
                channelId, //
                expiration));
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
