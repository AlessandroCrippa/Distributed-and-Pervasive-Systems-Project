package dps.adminserver.store;

import dps.common.OperationalState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class TelemetryRegistry {

    private final Map<Integer, List<TimestampedValue>> telemetryHistory = new HashMap<>();

    private final Map<Integer, OperationalState> currentStates = new HashMap<>();

    public synchronized void addTelemetry(int lineId, long timestamp, List<Double> averages) {
        telemetryHistory.putIfAbsent(lineId, new ArrayList<>());
        List<TimestampedValue> history = telemetryHistory.get(lineId);
        for (Double value : averages) {
            history.add(new TimestampedValue(timestamp, value));
        }
    }

    public synchronized void updateStates(int lineId, OperationalState state) {
        currentStates.put(lineId, state);
    }

    public synchronized List<TimestampedValue> getHistory(int lineId) {
        return new ArrayList<>(telemetryHistory.getOrDefault(lineId, new ArrayList<>()));
    }

    public synchronized OperationalState getState(int lineId) {
        return currentStates.get(lineId);

    }

    public static class TimestampedValue {
        private final long timestamp;
        private final double value;

        public TimestampedValue(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public double getValue() {
            return value;
        }
    }

    public synchronized Double getAverage(int lineId, long from, long to) {
        List<TimestampedValue> history = telemetryHistory.get(lineId);
        if (history == null) {
            return null;
        }

        double sum = 0;
        int count = 0;

        for (TimestampedValue t : history) {
            if (t.getTimestamp() >= from && t.getTimestamp() <= to) {
                sum += t.getValue();
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        return sum / count;
    }

}
