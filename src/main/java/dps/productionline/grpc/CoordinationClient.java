package dps.productionline.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class CoordinationClient {
    private final ManagedChannel channel;
    private final CoordinationServiceGrpc.CoordinationServiceBlockingStub stub;

    public CoordinationClient(String address, int port) {
        this.channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build();
        this.stub = CoordinationServiceGrpc.newBlockingStub(channel);
    }

    public void sendRequest(int lineId, double criticality) {
        CalibrationRequest request = CalibrationRequest.newBuilder().setLineId(lineId).setCriticality(criticality)
                .build();
        stub.requestCalibration(request);
    }

    public void sendReply(int lineId) {
        CalibrationReply reply = CalibrationReply.newBuilder().setLineId(lineId).build();
        stub.sendReply(reply);
    }

    public void announce(int lineId, String address, int port) {
        Announcement announcement = Announcement.newBuilder().setLineId(lineId).setAddress(address).setPort(port)
                .build();
        stub.announce(announcement);
    }

    public void notifyLeave(int lineId) {
        LeaveNotification notification = LeaveNotification.newBuilder().setLineId(lineId).build();
        stub.leavingNotification(notification);
    }

    public void shutdown() {
        channel.shutdown();
    }

}
