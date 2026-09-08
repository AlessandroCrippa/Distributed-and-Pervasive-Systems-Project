package dps.common;

public class MqttTopics {
    public static final String TELEMETRY_TOPIC = "smartfab/telemetry/";

    public static final String STATUS_TOPIC = "smartfab/status/";

    public static final String TELEMETRY_ALL = "smartfab/telemetry/+";
    public static final String STATUS_ALL = "smartfab/status/+";

    public static String telemetryTopic(int lineId) {
        return TELEMETRY_TOPIC + lineId;
    }

    public static String statusTopic(int lineId) {
        return STATUS_TOPIC + lineId;
    }

}
