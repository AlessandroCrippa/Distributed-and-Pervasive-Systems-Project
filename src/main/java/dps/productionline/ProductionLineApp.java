package dps.productionline;

import dps.common.OperationalState;
import dps.common.ProductionLineInfo;
import dps.productionline.rest.AdminServerClient;
import dps.productionline.sensor.Buffer;
import dps.productionline.sensor.Measurement;
import dps.productionline.sensor.MonitoringSensor;
import dps.productionline.sensor.SlidingWindowBuffer;
import io.grpc.ServerBuilder;
import io.grpc.Server;
import dps.productionline.mqtt.AveragesRegistry;
import dps.productionline.mqtt.TelemetryPublisher;
import dps.productionline.mqtt.StateUpdatePublisher;
import dps.productionline.grpc.CoordinationClient;
import dps.productionline.grpc.CoordinationServer;
import dps.productionline.grpc.RicartAgrawalaImplementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

public class ProductionLineApp {

    private static final double CRITICAL_THRESHOLD = 80.0;

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println(
                    "Write: ProductionLineApp <id> <ownAddress> <ownPort> <AdminServerAddress> <AdminServerPort>");
            return;
        }

        int id;
        String ownAddress;
        int ownPort;
        String adminServerAddress;
        int adminServerPort;

        try {
            id = Integer.parseInt(args[0]);
            ownAddress = args[1];
            ownPort = Integer.parseInt(args[2]);
            adminServerAddress = args[3];
            adminServerPort = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid argument has been written.");
            return;
        }

        ProductionLineInfo self = new ProductionLineInfo(id, ownAddress, ownPort);

        AdminServerClient client = new AdminServerClient(adminServerAddress, adminServerPort);

        Optional<List<ProductionLineInfo>> result = client.register(self);

        if (result.isEmpty()) {
            System.out.println("Registration failed: ID " + id + " is already in use.");
            return;
        }

        List<ProductionLineInfo> existingLines = result.get();
        System.out.println("Registration has been completed. Existing lines in the network: " + existingLines);

        Map<Integer, CoordinationClient> peers = new HashMap<>();
        for (ProductionLineInfo peerInfo : existingLines) {
            peers.put(peerInfo.getId(), new CoordinationClient(peerInfo.getAddress(), peerInfo.getPort()));
        }

        RicartAgrawalaImplementation ricartAgrawalaImp = new RicartAgrawalaImplementation(id, peers);

        Server grpcServer = ServerBuilder.forPort(ownPort).addService(new CoordinationServer(ricartAgrawalaImp))
                .build();
        try {
            grpcServer.start();
        } catch (java.io.IOException e) {
            System.out.println(
                    "Line " + id + " has failed to start gRPC server on the port " + ownPort + ": " + e.getMessage());
            client.removeLine(id);
            return;
        }

        System.out.println("Line " + id + " - gRPC server is starting on the port " + ownPort);

        Buffer buffer = new SlidingWindowBuffer();
        MonitoringSensor sensor = new MonitoringSensor(buffer);
        sensor.startMeasuring();

        List<Thread> announceThreads = new ArrayList<>();
        for (Map.Entry<Integer, CoordinationClient> entry : peers.entrySet()) {
            Thread t = new Thread(() -> {
                try {
                    entry.getValue().announce(id, ownAddress, ownPort);
                    System.out.println("Line " + id + " is announcing to line " + entry.getKey());
                } catch (Exception e) {
                    System.out.println("Line " + id + " has failed to announce to line " + entry.getKey());
                }
            });
            announceThreads.add(t);
            t.start();
        }

        for (Thread t : announceThreads) {
            t.join();
        }

        OperationalState[] stateHolder = { OperationalState.FULLY_OPERATIONAL };

        AveragesRegistry averagesRegistry = new AveragesRegistry();
        TelemetryPublisher telemetryPublisher = new TelemetryPublisher(id, averagesRegistry, () -> stateHolder[0]);
        telemetryPublisher.start();

        StateUpdatePublisher stateUpdatePublisher = new StateUpdatePublisher(id);
        stateUpdatePublisher.connect();

        boolean[] exitRequest = { false };
        ExitOperation exitOperation = new ExitOperation(id, () -> {
            exitRequest[0] = true;
            if (stateHolder[0] == OperationalState.WAITING_FOR_CALIBRATION) {
                ricartAgrawalaImp.cancelRequest();
            }
        });
        exitOperation.start();

        while (true) {
            if (exitRequest[0] && stateHolder[0] == OperationalState.FULLY_OPERATIONAL) {
                break;
            }

            Thread.sleep(200);

            List<Measurement> window = buffer.readAllAndClear();
            if (window == null) {
                continue;
            }

            double avg = window.stream().mapToDouble(Measurement::value).average().orElse(0.0);

            System.out.printf("Line %d - Average vibration: %.2f - state %s%n", id, avg, stateHolder[0]);

            averagesRegistry.addAverages(avg);

            if (stateHolder[0] == OperationalState.FULLY_OPERATIONAL && avg > CRITICAL_THRESHOLD) {
                double criticSituation = (avg - CRITICAL_THRESHOLD) / CRITICAL_THRESHOLD;
                stateHolder[0] = OperationalState.WAITING_FOR_CALIBRATION;
                System.out.printf(
                        "Line %d - critical condition has been detected! (%.2f > %.1f) =>state: WAITING_FOR_CALIBRATION (criticality: %.3f) %n",
                        id, avg,
                        CRITICAL_THRESHOLD, criticSituation);
                stateUpdatePublisher.publishStateUpdate(OperationalState.WAITING_FOR_CALIBRATION, criticSituation);

                sensor.pauseMeasuring();
                buffer.clear();

                boolean accessGranted = ricartAgrawalaImp.requestAccess(criticSituation);
                if (!accessGranted) {
                    System.out
                            .println("The calibration request of line " + id + " has been deleted due to exit request");
                    stateHolder[0] = OperationalState.FULLY_OPERATIONAL;
                    stateUpdatePublisher.publishStateUpdate(OperationalState.FULLY_OPERATIONAL, null);
                    break;
                }

                stateHolder[0] = OperationalState.UNDER_CALIBRATION;
                System.out.printf("Line %d is entering in UNDER_CALIBRATION%n", id);
                stateUpdatePublisher.publishStateUpdate(OperationalState.UNDER_CALIBRATION, null);
                long calibrationDuration = 3000 + (long) (Math.random() * 4000);
                Thread.sleep(calibrationDuration);

                ricartAgrawalaImp.releaseAccess();
                sensor.startMeasuring();
                stateHolder[0] = OperationalState.FULLY_OPERATIONAL;

                System.out.printf("Line %d has completed the calibration process and its state is FULLY_OPERATIONAL%n",
                        id);
                stateUpdatePublisher.publishStateUpdate(OperationalState.FULLY_OPERATIONAL, null);

            }
        }

        sensor.stopMeasuring();
        ricartAgrawalaImp.notifyAllPeersOfLeaving();
        client.removeLine(id);
        System.out.println("Line " + id + " has left the system");
        telemetryPublisher.stopPubblication();
        telemetryPublisher.disconnectFromMqtt();
        stateUpdatePublisher.disconnectFromMqtt();
        grpcServer.shutdown();
        ricartAgrawalaImp.shutDownAll();
    }
}
