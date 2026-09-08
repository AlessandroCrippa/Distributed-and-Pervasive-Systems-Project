package dps.adminserver.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import dps.common.ProductionLineInfo;
import org.springframework.stereotype.Service;

@Service
public class ProductionLineRegistry {
    public final Map<Integer, ProductionLineInfo> lines = new HashMap<>();

    public synchronized boolean addLine(ProductionLineInfo info) {
        if (lines.containsKey(info.getId())) {
            return false;
        }
        lines.put(info.getId(), info);
        return true;
    }

    public synchronized List<ProductionLineInfo> getAllLines() {
        return new ArrayList<>(lines.values());
    }

    public synchronized boolean removeLine(int id) {
        return lines.remove(id) != null;
    }
}
