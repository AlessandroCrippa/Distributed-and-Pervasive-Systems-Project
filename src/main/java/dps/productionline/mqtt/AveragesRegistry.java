package dps.productionline.mqtt;

import java.util.ArrayList;
import java.util.List;

public class AveragesRegistry {
    private final List<Double> averages = new ArrayList<>();

    public synchronized void addAverages(double average) {
        averages.add(average);
    }

    public synchronized List<Double> readAllAndClear() {
        List<Double> temp = new ArrayList<>(averages);
        averages.clear();
        return temp;
    }
}
