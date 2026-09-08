package dps.productionline.grpc;

import io.grpc.stub.StreamObserver;

public class CoordinationServer extends CoordinationServiceGrpc.CoordinationServiceImplBase {
    private final RicartAgrawalaImplementation ricartAgrawalaImplementation;

    public CoordinationServer(RicartAgrawalaImplementation ricartAgrawalaImplementation) {
        this.ricartAgrawalaImplementation = ricartAgrawalaImplementation;
    }

    @Override
    public void requestCalibration(CalibrationRequest request, StreamObserver<Ack> responseObserver) {
        ricartAgrawalaImplementation.onRequestReceived(request.getLineId(), request.getCriticality());

        responseObserver.onNext(Ack.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendReply(CalibrationReply reply, StreamObserver<Ack> responseObserver) {
        ricartAgrawalaImplementation.onReplyReceived(reply.getLineId());

        responseObserver.onNext(Ack.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void announce(Announcement announcement, StreamObserver<Ack> responseObserver) {
        CoordinationClient newPeerClient = new CoordinationClient(announcement.getAddress(), announcement.getPort());
        ricartAgrawalaImplementation.addPeer(announcement.getLineId(), newPeerClient);

        System.out.println("Announcemente from line " + announcement.getLineId());

        responseObserver.onNext(Ack.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void leavingNotification(LeaveNotification notification, StreamObserver<Ack> responseObserver) {
        ricartAgrawalaImplementation.removePeer(notification.getLineId());

        System.out.println("Line " + notification.getLineId() + " has left the system");

        responseObserver.onNext(Ack.newBuilder().build());
        responseObserver.onCompleted();
    }
}
