package org.phdezann.cn.core.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.phdezann.cn.core.Config;
import org.phdezann.cn.core.Config.ConfigKey;
import org.phdezann.cn.core.EventCreator;
import org.phdezann.cn.core.JsonSerializer;
import org.phdezann.cn.core.TerminationLock;

import lombok.extern.slf4j.Slf4j;

@Slf4j

public class MqttSubscriber extends AbstractMqttSubscriber {

    private final TerminationLock terminationLock;
    private final JsonSerializer jsonSerializer;
    private final EventCreator eventCreator;

    public MqttSubscriber(TerminationLock terminationLock, JsonSerializer jsonSerializer, Config config,
            EventCreator eventCreator) {
        super("ssl://" + config.get(ConfigKey.MQTT_HOSTNAME), //
                config.get(ConfigKey.MQTT_USERNAME), //
                config.get(ConfigKey.MQTT_PASSWORD), //
                List.of(config.get(ConfigKey.MQTT_TOPIC)));
        this.terminationLock = terminationLock;
        this.jsonSerializer = jsonSerializer;
        this.eventCreator = eventCreator;
    }

    @Override
    protected void messageArrived(String topic, MqttMessage message) {
        try {
            processMessage(topic, message);
        } catch (Exception ex) {
            log.error("Got exception", ex);
            terminationLock.signalAbnormalTermination();
            throw ex;
        }
    }

    record Payload(String channelId, String expiration) {
    }

    private void processMessage(String topic, MqttMessage message) {
        var jsonPayload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.trace("Received '{}' on topic '{}'", jsonPayload, topic);
        var payload = jsonSerializer.readValue(jsonPayload, Payload.class);
        this.eventCreator.onNotification(payload.channelId());
    }

}
