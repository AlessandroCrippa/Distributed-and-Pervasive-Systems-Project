package dps.adminserver.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;
import dps.adminserver.store.TelemetryRegistry;
import dps.common.MqttTopics;
import dps.common.OperationalState;
import dps.productionline.grpc.OperationalStateProto;
import dps.productionline.grpc.StateUpdateProtoMessage;
import jakarta.annotation.PostConstruct;

@Service
public class StateUpdateSubscriber {
    private static final String BROKER_ADDRESS = "tcp://localhost:1883";

    private final TelemetryRegistry telemetryRegistry;

    public StateUpdateSubscriber(TelemetryRegistry telemetryRegistry) {
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
                    handleStateUpdate(message.getPayload());
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("StateUpdateSubscriber has lost the connection: " + cause.getMessage());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            client.subscribe(MqttTopics.STATUS_ALL, 1);
            System.out.println("StateUpdateSubscriber has subscribed to " + MqttTopics.STATUS_ALL);

        } catch (Exception e) {
            System.out.println("StateUpdateSubscriber has failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleStateUpdate(byte[] payload) {
        try {
            StateUpdateProtoMessage message = StateUpdateProtoMessage.parseFrom(payload);
            OperationalState state = fromProto(message.getState());
            Double criticality = message.getHasCriticSituation() ? message.getCriticality() : null;

            telemetryRegistry.updateStates(message.getLineId(), state);

            System.out.println("StateUpdateSubscriber has received state update from line " + message.getLineId() + ": "
                    + state + " (criticality: " + criticality + ")");

        } catch (Exception e) {
            System.out.println(
                    "StateUpdateSubscriber has failed to parse the message: " + e.getMessage());
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
