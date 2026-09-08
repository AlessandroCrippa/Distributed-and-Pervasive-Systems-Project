package dps.productionline.sensor;

import java.util.ArrayList;
import java.util.List;

public class SlidingWindowBuffer implements Buffer {
    private static final int WINDOW_SIZE = 8;
    private static final int STEP = 4;

    private final List<Measurement> buffer = new ArrayList<>();
    private int measurementsSinceLastRead = 0;

    @Override
    public synchronized void addMeasurement(Measurement m) {
        buffer.add(m);
        if (buffer.size() > WINDOW_SIZE) {
            buffer.remove(0);
        }
        measurementsSinceLastRead++;
    }

    @Override
    public synchronized List<Measurement> readAllAndClear() {
        if (measurementsSinceLastRead < STEP) {
            return null;
        }
        measurementsSinceLastRead = 0;
        return new ArrayList<>(buffer);
    }

    @Override
    public synchronized void clear() {
        buffer.clear();
        measurementsSinceLastRead = 0;
    }

}
