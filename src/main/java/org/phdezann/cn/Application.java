package org.phdezann.cn;

import org.phdezann.cn.core.AppArgs;
import org.phdezann.cn.core.BulletCache;
import org.phdezann.cn.core.ChannelCache;
import org.phdezann.cn.core.ChannelLog;
import org.phdezann.cn.core.ConfigReader;
import org.phdezann.cn.core.DescriptionUpdater;
import org.phdezann.cn.core.EventCreator;
import org.phdezann.cn.core.EventFormatter;
import org.phdezann.cn.core.GoogleCalendar;
import org.phdezann.cn.core.GoogleClient;
import org.phdezann.cn.core.JsonSerializer;
import org.phdezann.cn.core.LinkParser;
import org.phdezann.cn.core.PushNotificationRenewer;
import org.phdezann.cn.core.SyncTokenCache;
import org.phdezann.cn.core.TerminationLock;
import org.phdezann.cn.core.converter.EventConverter;
import org.phdezann.cn.core.converter.EventDateTimeConverter;
import org.phdezann.cn.core.converter.EventStatusEnumConverter;
import org.phdezann.cn.core.mqtt.MqttSubscriber;
import org.phdezann.cn.wf.core.HttpClient;
import org.phdezann.cn.wf.core.RequestProxy;
import org.phdezann.cn.wf.core.request.CreatePushPollOperationBuilder;
import org.phdezann.cn.wf.core.request.EditPushPollOperationBuilder;

import com.beust.jcommander.JCommander;

public class Application {

    public static void main(String[] args) {
        AppArgs appArgs = new AppArgs();
        JCommander.newBuilder().addObject(appArgs).build().parse(args);

        var jsonDeserializer = new JsonSerializer();
        var configReader = new ConfigReader(appArgs, jsonDeserializer);
        var config = configReader.read();

        var googleClient = new GoogleClient(appArgs);
        var syncTokenCache = new SyncTokenCache(appArgs, jsonDeserializer);
        var googleCalendarClient = new GoogleCalendar(config, googleClient, syncTokenCache);
        var channelCache = new ChannelCache(appArgs, jsonDeserializer);
        var bulletCache = new BulletCache(appArgs, jsonDeserializer);
        var channelLog = new ChannelLog(appArgs, jsonDeserializer);
        var pushNotificationRenewer = new PushNotificationRenewer(config, googleCalendarClient, channelCache,
                channelLog);
        var eventFormatter = new EventFormatter(config);
        var linkParser = new LinkParser();
        var descriptionUpdater = new DescriptionUpdater(linkParser);
        var eventStatusEnumConverter = new EventStatusEnumConverter();
        var eventDateTimeConverter = new EventDateTimeConverter();
        var eventConverter = new EventConverter(eventStatusEnumConverter, eventDateTimeConverter);
        var httpClient = new HttpClient();

        var createPushPollDataBuilder = new CreatePushPollOperationBuilder(jsonDeserializer);
        var editPushPollDataBuilder = new EditPushPollOperationBuilder();
        var requestProxy = new RequestProxy(httpClient ,jsonDeserializer, createPushPollDataBuilder, editPushPollDataBuilder);
        var workflowyClient = new org.phdezann.cn.wf.core.WorkflowyClient(config, requestProxy);
        var eventCreator = new EventCreator(appArgs, googleCalendarClient, channelCache, eventStatusEnumConverter,
                eventConverter, eventFormatter, workflowyClient, linkParser, bulletCache, descriptionUpdater,
                jsonDeserializer);
        var terminationLock = new TerminationLock();
        var mqttSubscriber = new MqttSubscriber(terminationLock, jsonDeserializer, config, eventCreator);

        if (appArgs.isInitTokens()) {
            googleClient.renewTokens();
            return;
        }

        if (appArgs.isClearChannels()) {
            pushNotificationRenewer.clearAll();
        }

        pushNotificationRenewer.startScheduler();
        mqttSubscriber.startReadingMessagesAsync();
        eventCreator.createEventsOnStartup();
        eventCreator.setupEventsEveryDay();

        terminationLock.waitForAbnormalTermination();

        pushNotificationRenewer.shutdown();
        mqttSubscriber.shutdown();

        System.exit(1);
    }

}
