package dps.adminserver.mqtt;

import dps.adminserver.store.TelemetryRegistry;
import dps.common.MqttTopics;
import dps.common.OperationalState;
import dps.productionline.grpc.OperationalStateProto;
import dps.productionline.grpc.TelemetryProtoMessage;
import jakarta.annotation.PostConstruct;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TelemetrySubscriber {
    private static final String BROKER_ADDRESS = "tcp://localhost:1883";

    private final TelemetryRegistry telemetryRegistry;

    public TelemetrySubscriber(TelemetryRegistry telemetryRegistry) {
        this.telemetryRegistry = telemetryRegistry;
    }

    @PostConstruct
    public void start() {
        try {
            String clientId = MqttClient.generateClientId();
            MqttClient client = new MqttClient(BROKER_ADDRESS, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);

            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleTelemetryMessage(message.getPayload());
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("TelemetrySubscriber has lost the connection: " + cause.getMessage());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            client.subscribe(MqttTopics.TELEMETRY_ALL, 1);
            System.out.println("TelemetrySubscriber has subscribed to " + MqttTopics.TELEMETRY_ALL);

        } catch (Exception e) {
            System.out.println("TelemetrySubscriber has failed to start: " + e.getMessage());
        }
    }

    private void handleTelemetryMessage(byte[] payload) {
        try {
            TelemetryProtoMessage message = TelemetryProtoMessage.parseFrom(payload);
            OperationalState state = fromProto(message.getState());
            List<Double> averages = message.getAveragesList();

            telemetryRegistry.addTelemetry(message.getLineId(), message.getTimestamp(), averages);
            telemetryRegistry.updateStates(message.getLineId(), state);

            System.out.println("TelemetrySubscriber has received telemetry from line " + message.getLineId() + ": "
                    + averages);
        } catch (Exception e) {
            System.out
                    .println("TelemetrySubscriber has failed to parse the message: " + payload + " --- "
                            + e.getMessage());
        }
    }

    private OperationalState fromProto(OperationalStateProto stateProto) {
        return switch (stateProto) {
            case FULLY_OPERATIONAL -> OperationalState.FULLY_OPERATIONAL;
            case WAITING_FOR_CALIBRATION -> OperationalState.WAITING_FOR_CALIBRATION;
            case UNDER_CALIBRATION -> OperationalState.UNDER_CALIBRATION;
            default -> throw new IllegalArgumentException("Unrecognized state: " + stateProto);
        };
    }
}
