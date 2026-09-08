package dps.productionline.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import dps.common.OperationalState;
import dps.common.MqttTopics;
import dps.productionline.grpc.OperationalStateProto;
import dps.productionline.grpc.StateUpdateProtoMessage;

public class StateUpdatePublisher {
    private static final String BROKER_ADDRESS = "tcp://localhost:1883";

    private final int lineId;
    private MqttClient client;

    public StateUpdatePublisher(int lineId) {
        this.lineId = lineId;
    }

    public void connect() throws Exception {
        String clientId = MqttClient.generateClientId();
        client = new MqttClient(BROKER_ADDRESS, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);
        System.out.println("Line " + lineId + " StateUpdatePublisher is connected to MQTT broker");
    }

    public void publishStateUpdate(OperationalState newState, Double criticSituation) {
        OperationalStateProto stateProto = toProto(newState);
        StateUpdateProtoMessage.Builder builder = StateUpdateProtoMessage.newBuilder().setLineId(lineId)
                .setState(stateProto).setTimestamp(System.currentTimeMillis());

        if (criticSituation != null) {
            builder.setCriticality(criticSituation);
            builder.setHasCriticSituation(true);
        } else {
            builder.setHasCriticSituation(false);
        }

        StateUpdateProtoMessage message = builder.build();

        try {
            byte[] payload = message.toByteArray();
            MqttMessage mqttMessage = new MqttMessage(payload);
            mqttMessage.setQos(1);
            client.publish(MqttTopics.statusTopic(lineId), mqttMessage);
            System.out.println("Line " + lineId + " has published state update: " + message);
        } catch (Exception e) {
            System.out.println("Line " + lineId + " has failed to publish state update: " + e.getMessage());
        }
    }

    private OperationalStateProto toProto(OperationalState state) {
        return switch (state) {
            case FULLY_OPERATIONAL -> OperationalStateProto.FULLY_OPERATIONAL;
            case WAITING_FOR_CALIBRATION -> OperationalStateProto.WAITING_FOR_CALIBRATION;
            case UNDER_CALIBRATION -> OperationalStateProto.UNDER_CALIBRATION;
        };
    }

    public void disconnectFromMqtt() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception e) {
            System.out.println("Line " + lineId + " has failed to disconnect MQTT: " + e.getMessage());
        }
    }
}
