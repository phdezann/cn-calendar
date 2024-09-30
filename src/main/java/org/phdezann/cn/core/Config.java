package org.phdezann.cn.core;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Config {

    @SuppressWarnings("unused")
    public enum ConfigKey {
        MQTT_HOSTNAME,
        MQTT_USERNAME,
        MQTT_PASSWORD,
        MQTT_TOPIC,
        CALENDAR_WEBHOOK,
        CALENDAR_WEBHOOK_TOKEN,
        CALENDAR_TITLES, ENABLE_HOSTNAME_IN_TITLE,
        WORKFLOWY_SHARE_SECRET_LINK,
    }

    private Map<ConfigKey, String> entries = new HashMap<>();

    public String get(ConfigKey key) {
        return entries.get(key);
    }

    public boolean isTrue(ConfigKey key) {
        return "true".equalsIgnoreCase(entries.get(key));
    }

}
