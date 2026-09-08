package dps.productionline.grpc;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class RicartAgrawalaImplementation {
    private final int selfId;
    private final Map<Integer, CoordinationClient> peers;

    private double myCriticSituation = 0.0;
    private boolean requestOrInCalibration = false;
    private boolean cancel = false;

    private int receivedReplies = 0;
    private final List<Integer> defQueue = new ArrayList<>();

    public RicartAgrawalaImplementation(int selfId, Map<Integer, CoordinationClient> peers) {
        this.selfId = selfId;
        this.peers = peers;
    }

    public boolean requestAccess(double criticality) throws InterruptedException {
        List<CoordinationClient> targets;
        int expectedReplies;

        synchronized (this) {
            this.myCriticSituation = criticality;
            this.requestOrInCalibration = true;
            this.receivedReplies = 0;
            this.cancel = false;
            targets = new ArrayList<>(peers.values());
            expectedReplies = targets.size();
        }

        List<Thread> threads = new ArrayList<>();

        for (CoordinationClient peerClient : targets) {
            Thread t = new Thread(() -> safeSendRequest(peerClient, criticality));
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        synchronized (this) {
            while (receivedReplies < expectedReplies && !cancel) {
                wait();
            }
            return !cancel;
        }
    }

    public synchronized void onReplyReceived(int fromLineId) {
        receivedReplies++;
        notifyAll();
    }

    public void onRequestReceived(int requesterId, double requesterCriticSituation) {

        boolean ifDefer;
        CoordinationClient requesterClient = null;

        synchronized (this) {
            ifDefer = requestOrInCalibration && hasPriorityOverRequester(requesterCriticSituation, requesterId);
            if (ifDefer) {
                defQueue.add(requesterId);
            } else {
                requesterClient = peers.get(requesterId);
            }
        }

        if (!ifDefer && requesterClient != null) {
            safeSendReply(requesterClient);
        }
    }

    public void releaseAccess() {
        List<CoordinationClient> clientsToNotify;
        synchronized (this) {
            requestOrInCalibration = false;
            clientsToNotify = new ArrayList<>();
            for (int waitingLineId : defQueue) {
                CoordinationClient client = peers.get(waitingLineId);
                if (client != null) {
                    clientsToNotify.add(client);
                }
            }
            defQueue.clear();
        }

        List<Thread> threads = new ArrayList<>();
        for (CoordinationClient waitingClient : clientsToNotify) {
            Thread t = new Thread(() -> safeSendReply(waitingClient));
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void cancelRequest() {
        requestOrInCalibration = false;
        cancel = true;
        notifyAll();
    }

    public synchronized void addPeer(int peerId, CoordinationClient peerClient) {
        peers.put(peerId, peerClient);
    }

    public synchronized void removePeer(int peerId) {
        peers.remove(peerId);
        defQueue.remove(Integer.valueOf(peerId));
    }

    public void notifyAllPeersOfLeaving() {
        List<CoordinationClient> targets;
        synchronized (this) {
            targets = new ArrayList<>(peers.values());
        }
        List<Thread> threads = new ArrayList<>();
        for (CoordinationClient peerClient : targets) {
            Thread t = new Thread(() -> safeNotifyLeave(peerClient));
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void safeSendRequest(CoordinationClient client, double criticality) {
        try {
            client.sendRequest(selfId, criticality);
        } catch (Exception e) {
            System.out.println("Line " + selfId + " has failed to send request to a peer: " + e.getMessage());
        }
    }

    private void safeSendReply(CoordinationClient client) {
        try {
            client.sendReply(selfId);
        } catch (Exception e) {
            System.out.println("Line " + selfId + " has failed to send reply to a peer: " + e.getMessage());
        }
    }

    private void safeNotifyLeave(CoordinationClient client) {
        try {
            client.notifyLeave(selfId);
        } catch (Exception e) {
            System.out.println("Line " + selfId + " has failed to notify a peer about leaving: " + e.getMessage());
        }
    }

    private boolean hasPriorityOverRequester(double requesterCriticSituation, int requesterId) {
        if (myCriticSituation > requesterCriticSituation) {
            return true;
        }

        if (myCriticSituation == requesterCriticSituation) {
            return selfId > requesterId;
        }

        return false;
    }

    public void shutDownAll() {
        synchronized (this) {
            for (CoordinationClient client : peers.values()) {
                client.shutdown();
            }
        }
    }
}
