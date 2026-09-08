package dps.productionline.mqtt;

import dps.common.MqttTopics;
import dps.common.OperationalState;
import dps.productionline.grpc.OperationalStateProto;
import dps.productionline.grpc.TelemetryProtoMessage;

import java.util.function.Supplier;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class TelemetryPublisher extends Thread {
    private static final String BROKER_ADDRESS = "tcp://localhost:1883";
    private static final long PUBLISH_INTERVAL = 10_000;

    private final int lineId;
    private final AveragesRegistry averagesRegistry;
    private final Supplier<OperationalState> stateSupplier;

    private MqttClient client;
    private volatile boolean running = true;

    public TelemetryPublisher(int lineId, AveragesRegistry averagesRegistry, Supplier<OperationalState> stateSupplier) {
        this.lineId = lineId;
        this.averagesRegistry = averagesRegistry;
        this.stateSupplier = stateSupplier;
    }

    @Override
    public void run() {
        try {
            String clientId = MqttClient.generateClientId();
            client = new MqttClient(BROKER_ADDRESS, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            System.out.println("Line " + lineId + " is connected to MQTT broker");

            while (running) {
                Thread.sleep(PUBLISH_INTERVAL);
                publishTelemetry();
            }
        } catch (MqttException e) {
            System.out.println("Line " + lineId + " generates MQTT error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void publishTelemetry() {
        List<Double> averages = averagesRegistry.readAllAndClear();
        if (averages.isEmpty()) {
            return;
        }

        OperationalStateProto stateProto = toProto(stateSupplier.get());

        TelemetryProtoMessage message = TelemetryProtoMessage.newBuilder().setLineId(lineId)
                .setTimestamp(System.currentTimeMillis()).setState(stateProto).addAllAverages(averages).build();

        try {
            byte[] payload = message.toByteArray();
            MqttMessage mqttMessage = new MqttMessage(payload);
            mqttMessage.setQos(1);
            client.publish(MqttTopics.telemetryTopic(lineId), mqttMessage);
            System.out.println("Line " + lineId + " has published telemetry: " + message);
        } catch (Exception e) {
            System.out.println("Line " + lineId + " has failed to publish telemetry: " + e.getMessage());
        }
    }

    private OperationalStateProto toProto(OperationalState state) {
        return switch (state) {
            case FULLY_OPERATIONAL -> OperationalStateProto.FULLY_OPERATIONAL;
            case WAITING_FOR_CALIBRATION -> OperationalStateProto.WAITING_FOR_CALIBRATION;
            case UNDER_CALIBRATION -> OperationalStateProto.UNDER_CALIBRATION;
        };
    }

    public void stopPubblication() {
        running = false;
        this.interrupt();
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
